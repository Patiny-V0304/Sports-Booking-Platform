import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';
import { MapPin, Calendar, Lock, CheckCircle2, RefreshCw, XCircle, QrCode } from 'lucide-react';

export default function BookingPage() {
    const [facilities, setFacilities] = useState([]);
    const [selectedFacility, setSelectedFacility] = useState(null);
    const [playDate, setPlayDate] = useState('2026-10-30');
    const [slots, setSlots] = useState([]);
    const [selectedSlot, setSelectedSlot] = useState(null);

    const [voucherCode, setVoucherCode] = useState('');
    const [hasMatchmaking, setHasMatchmaking] = useState(true);
    const [activeBooking, setActiveBooking] = useState(null);
    const [cancelPreview, setCancelPreview] = useState(null);
    const [newSlotIdForReschedule, setNewSlotIdForReschedule] = useState('');
    const [msg, setMsg] = useState({ type: '', text: '' });

    const loadNearbyFacilities = async () => {
        try {
            const res = await axiosClient.get('/facilities/nearby?lat=10.8018&lng=106.7144&radiusKm=15');
            setFacilities(res.data || []);
            if (res.data?.length > 0 && !selectedFacility) {
                setSelectedFacility(res.data[0]);
            }
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const loadSlots = async (facilityId, date) => {
        if (!facilityId) return;
        try {
            const res = await axiosClient.get(`/facilities/${facilityId}/slots?playDate=${date}`);
            setSlots(res.data || []);
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    useEffect(() => {
        loadNearbyFacilities();
    }, []);

    useEffect(() => {
        if (selectedFacility) {
            loadSlots(selectedFacility.id, playDate);
        }
    }, [selectedFacility, playDate]);

    // 1. Bấm Đặt sân -> Khóa Redis 5 phút & Tính cọc 50%
    const handleCreateBooking = async () => {
        if (!selectedSlot) return;
        setMsg({ type: '', text: '' });
        try {
            const res = await axiosClient.post('/bookings', {
                slotId: selectedSlot.slotId,
                hasMatchmaking,
                voucherCode: voucherCode.trim() || null,
                paymentGateway: 'VNPAY',
            });
            setActiveBooking(res.data.booking);
            setMsg({
                type: 'success',
                text: `Đã khóa tạm Slot trên Redis (TTL 5 phút)! Mã đơn: ${res.data.booking.bookingCode}`,
            });
            loadSlots(selectedFacility.id, playDate);
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    // 2. Giả lập cổng thanh toán VNPay gọi Webhook xác nhận thành công 50% cọc
    const handleSimulateWebhook = async (responseCode) => {
        if (!activeBooking) return;
        try {
            const res = await axiosClient.post('/payments/webhook', {
                bookingCode: activeBooking.bookingCode,
                gatewayTxnRef: 'VNPAY-' + Date.now(),
                responseCode,
            });
            setActiveBooking(res.data);
            setMsg({
                type: responseCode === '00' ? 'success' : 'error',
                text:
                    responseCode === '00'
                        ? 'Thanh toán cọc 50% thành công! Đơn chuyển sang BOOKED & đã sinh mã QR Check-in.'
                        : 'Thanh toán thất bại! Đã hủy đơn PENDING và mở khóa Redis.',
            });
            loadSlots(selectedFacility.id, playDate);
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    // 3. Xem trước chính sách hoàn cọc
    const handlePreviewCancel = async (reason = 'USER_CANCEL') => {
        if (!activeBooking) return;
        try {
            const res = await axiosClient.get(`/bookings/${activeBooking.id}/cancel-preview?reason=${reason}`);
            setCancelPreview({ ...res.data, selectedReason: reason });
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    // 4. Xác nhận Hủy đơn đặt sân
    const handleConfirmCancel = async () => {
        if (!activeBooking || !cancelPreview) return;
        try {
            const res = await axiosClient.post(`/bookings/${activeBooking.id}/cancel`, {
                cancelReason: cancelPreview.selectedReason,
            });
            setActiveBooking(res.data);
            setCancelPreview(null);
            setMsg({
                type: 'success',
                text: `Đã hủy đơn! Số tiền cọc hoàn lại: ${res.data.refundAmount?.toLocaleString()} VNĐ (Đã bắn sự kiện RabbitMQ đóng phòng ghép kèo).`,
            });
            loadSlots(selectedFacility.id, playDate);
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    // 5. Đổi lịch miễn phí 1 lần
    const handleReschedule = async () => {
        if (!activeBooking || !newSlotIdForReschedule) return;
        try {
            const res = await axiosClient.put(
                `/bookings/${activeBooking.id}/reschedule?newSlotId=${newSlotIdForReschedule}`
            );
            setActiveBooking(res.data);
            setMsg({ type: 'success', text: 'Đổi lịch sân miễn phí (lần 1) thành công!' });
            loadSlots(selectedFacility.id, playDate);
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    return (
        <div className="max-w-7xl mx-auto p-6 space-y-6">
            {msg.text && (
                <div
                    className={`p-4 rounded-xl font-medium ${
                        msg.type === 'error' ? 'bg-red-50 text-red-700 border border-red-200' : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    }`}
                >
                    {msg.text}
                </div>
            )}

            <div className="grid lg:grid-cols-3 gap-6">
                {/* Cột 1 & 2: Danh sách Cụm sân GPS & Bảng chọn Slot */}
                <div className="lg:col-span-2 space-y-6">
                    <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                        <div className="flex items-center justify-between mb-4">
                            <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                <MapPin className="w-5 h-5 text-emerald-600" />
                                1. Cụm Sân Thể Thao Quanh Bạn (GPS PostGIS)
                            </h2>
                            <button
                                onClick={loadNearbyFacilities}
                                className="text-xs bg-slate-100 hover:bg-slate-200 px-3 py-1.5 rounded-lg font-medium cursor-pointer"
                            >
                                Làm mới danh sách
                            </button>
                        </div>

                        {facilities.length === 0 ? (
                            <p className="text-sm text-slate-500">
                                Chưa có cụm sân nào ở trạng thái ACTIVE. Hãy qua tab "Quản Lý Sân & Admin" để tạo và duyệt sân trước!
                            </p>
                        ) : (
                            <div className="grid sm:grid-cols-2 gap-3">
                                {facilities.map((fac) => (
                                    <div
                                        key={fac.id}
                                        onClick={() => setSelectedFacility(fac)}
                                        className={`p-4 rounded-xl border cursor-pointer transition ${
                                            selectedFacility?.id === fac.id
                                                ? 'border-emerald-600 bg-emerald-50/50'
                                                : 'border-slate-200 hover:border-emerald-300'
                                        }`}
                                    >
                                        <div className="flex justify-between items-start">
                                            <h3 className="font-bold text-slate-800">{fac.name}</h3>
                                            <span className="text-xs bg-amber-100 text-amber-800 px-2 py-0.5 rounded font-bold">
                        ★ {fac.ratingAvg}
                      </span>
                                        </div>
                                        <p className="text-xs text-slate-500 mt-1">{fac.address}</p>
                                        <p className="text-xs text-emerald-700 font-medium mt-2">
                                            {fac.isOutdoor ? '🌤 Sân ngoài trời (Hỗ trợ hủy thời tiết)' : '🏠 Sân trong nhà'}
                                        </p>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* Sơ đồ Khung giờ (Slots) */}
                    {selectedFacility && (
                        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                            <div className="flex flex-wrap items-center justify-between gap-4 mb-4">
                                <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                    <Calendar className="w-5 h-5 text-emerald-600" />
                                    2. Chọn Khung Giờ Sân ({selectedFacility.name})
                                </h2>
                                <input
                                    type="date"
                                    value={playDate}
                                    onChange={(e) => setPlayDate(e.target.value)}
                                    className="border border-slate-300 rounded-lg px-3 py-1.5 text-sm font-medium"
                                />
                            </div>

                            <div className="flex gap-4 text-xs mb-4">
                <span className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded-full bg-emerald-500 inline-block"></span> AVAILABLE (Còn trống)
                </span>
                                <span className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded-full bg-amber-500 inline-block"></span> LOCKED (Đang khóa Redis 5p)
                </span>
                                <span className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded-full bg-red-500 inline-block"></span> BOOKED (Đã đặt)
                </span>
                            </div>

                            {slots.length === 0 ? (
                                <p className="text-sm text-slate-500">Không có khung giờ nào trong ngày {playDate}.</p>
                            ) : (
                                <div className="grid sm:grid-cols-2 md:grid-cols-3 gap-3">
                                    {slots.map((slot) => {
                                        const isAvailable = slot.status === 'AVAILABLE';
                                        const isSelected = selectedSlot?.slotId === slot.slotId;
                                        return (
                                            <div
                                                key={slot.slotId}
                                                onClick={() => isAvailable && setSelectedSlot(slot)}
                                                className={`p-3.5 rounded-xl border text-sm transition ${
                                                    slot.status === 'BOOKED'
                                                        ? 'bg-red-50 border-red-200 text-red-800 opacity-75 not-allowed'
                                                        : slot.status === 'LOCKED'
                                                            ? 'bg-amber-50 border-amber-300 text-amber-900'
                                                            : isSelected
                                                                ? 'bg-emerald-600 text-white border-emerald-600 shadow-md cursor-pointer'
                                                                : 'bg-white border-slate-200 hover:border-emerald-500 cursor-pointer'
                                                }`}
                                            >
                                                <div className="flex justify-between items-center font-bold">
                                                    <span>{slot.courtName}</span>
                                                    <span className="text-xs px-2 py-0.5 rounded bg-black/10">{slot.sportType}</span>
                                                </div>
                                                <p className="mt-1 font-semibold">
                                                    {slot.startTime} - {slot.endTime}
                                                </p>
                                                <div className="mt-2 flex justify-between items-center text-xs">
                                                    <span className="font-bold">{Number(slot.price).toLocaleString()} đ</span>
                                                    <span className="font-semibold">{slot.status}</span>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    )}
                </div>

                {/* Cột 3: Bảng Tính Tiền Cọc 50%, Thanh Toán & Quản Lý Đơn */}
                <div className="space-y-6">
                    <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 space-y-4">
                        <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <Lock className="w-5 h-5 text-emerald-600" />
                            3. Đặt Sân & Thanh Toán Cọc 50%
                        </h2>

                        {selectedSlot ? (
                            <div className="bg-slate-50 p-3.5 rounded-xl text-sm space-y-1 border border-slate-200">
                                <p><strong>Sân:</strong> {selectedSlot.courtName} ({selectedSlot.sportType})</p>
                                <p><strong>Khung giờ:</strong> {selectedSlot.startTime} - {selectedSlot.endTime} ({selectedSlot.playDate})</p>
                                <p><strong>Giá gốc (100%):</strong> <span className="text-emerald-700 font-bold">{Number(selectedSlot.price).toLocaleString()} VNĐ</span></p>
                            </div>
                        ) : (
                            <p className="text-sm text-slate-500">Hãy chọn 1 khung giờ trống (AVAILABLE) ở bên trái.</p>
                        )}

                        <div>
                            <label className="block text-xs font-semibold text-slate-600 mb-1">
                                Mã giảm giá (Voucher Chủ sân hoặc Khuyến mãi Sàn)
                            </label>
                            <input
                                type="text"
                                value={voucherCode}
                                onChange={(e) => setVoucherCode(e.target.value)}
                                placeholder="VD: SANVIP20 hoặc ADMIN50K"
                                className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                            />
                        </div>

                        <label className="flex items-center gap-2 text-sm text-slate-700 cursor-pointer">
                            <input
                                type="checkbox"
                                checked={hasMatchmaking}
                                onChange={(e) => setHasMatchmaking(e.target.checked)}
                                className="rounded text-emerald-600"
                            />
                            Bật tính năng mở phòng ghép đội (Matchmaking)
                        </label>

                        <button
                            disabled={!selectedSlot}
                            onClick={handleCreateBooking}
                            className="w-full bg-emerald-600 disabled:bg-slate-300 hover:bg-emerald-700 text-white font-semibold py-2.5 rounded-lg cursor-pointer"
                        >
                            Khóa Slot Redis (5 phút) & Tạo Đơn
                        </button>
                    </div>

                    {/* Chi tiết đơn đặt sân vừa tạo */}
                    {activeBooking && (
                        <div className="bg-white p-6 rounded-2xl shadow-sm border border-emerald-300 space-y-4">
                            <div className="flex justify-between items-center">
                                <h3 className="font-bold text-slate-800">Đơn: {activeBooking.bookingCode}</h3>
                                <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800">
                  {activeBooking.status}
                </span>
                            </div>

                            <div className="text-xs bg-slate-50 p-3 rounded-xl space-y-1.5 border border-slate-200">
                                <p><strong>Booking ID:</strong> <code className="bg-slate-200 px-1 rounded">{activeBooking.id}</code></p>
                                <p><strong>Giá sân gốc:</strong> {Number(activeBooking.courtPrice).toLocaleString()} đ</p>
                                <p><strong>Giảm giá Chủ sân:</strong> -{Number(activeBooking.facilityDiscount).toLocaleString()} đ</p>
                                <p><strong>Khuyến mãi Sàn:</strong> -{Number(activeBooking.platformDiscount).toLocaleString()} đ</p>
                                <hr className="my-1 border-slate-200" />
                                <p className="text-sm text-emerald-700 font-bold">
                                    Tiền cọc 50% (Cổng TT): {Number(activeBooking.depositAmount).toLocaleString()} đ
                                </p>
                                <p className="text-sm text-amber-700 font-bold">
                                    50% còn lại trả tại sân: {Number(activeBooking.remainingAmount).toLocaleString()} đ
                                </p>
                            </div>

                            {/* Nếu đơn đang PENDING -> Hiện nút giả lập Webhook VNPay */}
                            {activeBooking.status === 'PENDING' && (
                                <div className="space-y-2 pt-2">
                                    <p className="text-xs font-semibold text-amber-700">
                                        ⏳ Slot đang khóa trên Redis 5 phút. Bấm nút dưới để giả lập Webhook từ cổng thanh toán:
                                    </p>
                                    <div className="grid grid-cols-2 gap-2">
                                        <button
                                            onClick={() => handleSimulateWebhook('00')}
                                            className="flex items-center justify-center gap-1 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold py-2.5 rounded-lg cursor-pointer"
                                        >
                                            <CheckCircle2 className="w-4 h-4" />
                                            TT Thành Công (00)
                                        </button>
                                        <button
                                            onClick={() => handleSimulateWebhook('24')}
                                            className="flex items-center justify-center gap-1 bg-red-600 hover:bg-red-700 text-white text-xs font-bold py-2.5 rounded-lg cursor-pointer"
                                        >
                                            <XCircle className="w-4 h-4" />
                                            Hủy / Thất Bại
                                        </button>
                                    </div>
                                </div>
                            )}

                            {/* Nếu đơn đã BOOKED -> Hiện mã QR Check-in + Đổi lịch + Hủy đơn */}
                            {activeBooking.status === 'BOOKED' && (
                                <div className="space-y-4 pt-2 border-t border-slate-200">
                                    <div className="text-center bg-slate-50 p-3 rounded-xl border border-slate-200">
                                        <p className="text-xs font-bold text-slate-700 mb-2 flex items-center justify-center gap-1">
                                            <QrCode className="w-4 h-4 text-emerald-600" />
                                            Mã QR Check-in tại sân
                                        </p>
                                        <img
                                            src={`https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=${activeBooking.qrCheckinToken}`}
                                            alt="QR Check-in"
                                            className="mx-auto rounded-lg border border-slate-300"
                                        />
                                        <p className="text-xs font-mono text-slate-600 mt-2 select-all">
                                            {activeBooking.qrCheckinToken}
                                        </p>
                                    </div>

                                    {/* Đổi lịch miễn phí 1 lần */}
                                    <div className="space-y-2">
                                        <label className="block text-xs font-bold text-slate-700">
                                            Đổi lịch miễn phí (Tối đa 1 lần trước 1h):
                                        </label>
                                        <div className="flex gap-2">
                                            <select
                                                value={newSlotIdForReschedule}
                                                onChange={(e) => setNewSlotIdForReschedule(e.target.value)}
                                                className="flex-1 border border-slate-300 rounded-lg px-2 py-1.5 text-xs bg-white"
                                            >
                                                <option value="">-- Chọn khung giờ trống mới --</option>
                                                {slots
                                                    .filter((s) => s.status === 'AVAILABLE')
                                                    .map((s) => (
                                                        <option key={s.slotId} value={s.slotId}>
                                                            {s.courtName}: {s.startTime}-{s.endTime} ({Number(s.price).toLocaleString()}đ)
                                                        </option>
                                                    ))}
                                            </select>
                                            <button
                                                onClick={handleReschedule}
                                                className="bg-slate-800 text-white text-xs font-semibold px-3 py-1.5 rounded-lg flex items-center gap-1 cursor-pointer"
                                            >
                                                <RefreshCw className="w-3.5 h-3.5" /> Đổi lịch
                                            </button>
                                        </div>
                                    </div>

                                    {/* Kiểm tra chính sách hoàn cọc & Hủy đơn */}
                                    <div className="space-y-2 pt-2 border-t border-slate-200">
                                        <p className="text-xs font-bold text-slate-700">Kiểm tra mức hoàn cọc khi Hủy đơn:</p>
                                        <div className="flex flex-wrap gap-1.5">
                                            <button
                                                onClick={() => handlePreviewCancel('USER_CANCEL')}
                                                className="text-xs bg-slate-100 hover:bg-slate-200 px-2.5 py-1.5 rounded font-medium cursor-pointer"
                                            >
                                                Khách tự hủy
                                            </button>
                                            <button
                                                onClick={() => handlePreviewCancel('INSUFFICIENT_PLAYERS_4H')}
                                                className="text-xs bg-amber-100 hover:bg-amber-200 text-amber-900 px-2.5 py-1.5 rounded font-medium cursor-pointer"
                                            >
                                                Mốc 4h thiếu người
                                            </button>
                                            <button
                                                onClick={() => handlePreviewCancel('WEATHER_CANCEL')}
                                                className="text-xs bg-blue-100 hover:bg-blue-200 text-blue-900 px-2.5 py-1.5 rounded font-medium cursor-pointer"
                                            >
                                                Hủy do thời tiết
                                            </button>
                                        </div>

                                        {cancelPreview && (
                                            <div className="bg-red-50 border border-red-200 p-3 rounded-xl text-xs space-y-1.5">
                                                <p><strong>Quy tắc áp dụng:</strong> {cancelPreview.appliedPolicyRule}</p>
                                                <p><strong>Tỷ lệ hoàn cọc:</strong> <span className="font-bold text-red-700">{cancelPreview.refundPercentage}%</span></p>
                                                <p><strong>Số tiền hoàn lại:</strong> <span className="font-bold text-emerald-700">{Number(cancelPreview.estimatedRefundAmount).toLocaleString()} VNĐ</span></p>
                                                <button
                                                    onClick={handleConfirmCancel}
                                                    className="w-full mt-2 bg-red-600 hover:bg-red-700 text-white font-bold py-2 rounded-lg cursor-pointer"
                                                >
                                                    Xác nhận Hủy Đơn & Hoàn Tiền
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}