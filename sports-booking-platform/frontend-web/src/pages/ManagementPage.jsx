import { useState } from 'react';
import axiosClient from '../api/axiosClient';
import { Building2, ShieldAlert, QrCode, TicketPercent, BarChart3 } from 'lucide-react';

export default function ManagementPage() {
    const [msg, setMsg] = useState({ type: '', text: '' });

    // 1. Đăng ký cụm sân
    const [facForm, setFacForm] = useState({
        name: 'Cụm Sân Pickleball & Cầu Lông Thủ Đức',
        address: '240 Võ Văn Ngân, TP. Thủ Đức, TP.HCM',
        latitude: 10.8505,
        longitude: 106.7717,
        businessLicenseUrl: 'https://example.com/license.pdf',
        isOutdoor: true,
    });
    const [createdFacilityId, setCreatedFacilityId] = useState('');

    // 2. Tạo sân con & khung giờ
    const [courtForm, setCourtForm] = useState({
        facilityId: '',
        courtName: 'Sân Pickleball Tiêu Chuẩn 01',
        sportType: 'PICKLEBALL',
        playDate: '2026-10-30',
    });

    // 3. Quét QR Check-in & Chốt doanh thu
    const [qrTokenInput, setQrTokenInput] = useState('');
    const [bookingIdForComplete, setBookingIdForComplete] = useState('');
    const [revenueReport, setRevenueReport] = useState(null);

    // 4. Tạo Voucher
    const [voucherForm, setVoucherForm] = useState({
        code: 'SANVIP20',
        issuerType: 'FACILITY',
        facilityId: '',
        discountPercent: 10,
        maxDiscountAmount: 20000,
        validTo: '2026-12-31T23:59:59',
        usageLimit: 100,
    });

    const handleRegisterFacility = async (e) => {
        e.preventDefault();
        try {
            const res = await axiosClient.post('/facilities/register', facForm);
            setCreatedFacilityId(res.data.id);
            setCourtForm({ ...courtForm, facilityId: res.data.id });
            setVoucherForm({ ...voucherForm, facilityId: res.data.id });
            setMsg({
                type: 'success',
                text: `Đã gửi hồ sơ cụm sân! Facility ID: ${res.data.id} (Trạng thái: PENDING_APPROVAL)`,
            });
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const handleAdminApprove = async (status) => {
        if (!createdFacilityId) return;
        try {
            await axiosClient.put(`/admin/facilities/${createdFacilityId}/approval?status=${status}`);
            setMsg({ type: 'success', text: `Admin đã cập nhật trạng thái cụm sân sang ${status}!` });
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const handleCreateCourtAndSlots = async (e) => {
        e.preventDefault();
        try {
            await axiosClient.post(`/facilities/${courtForm.facilityId}/courts`, {
                courtName: courtForm.courtName,
                sportType: courtForm.sportType,
                slots: [
                    { playDate: courtForm.playDate, startTime: '17:00:00', endTime: '19:00:00', price: 200000 },
                    { playDate: courtForm.playDate, startTime: '19:00:00', endTime: '21:00:00', price: 200000 },
                    { playDate: courtForm.playDate, startTime: '21:00:00', endTime: '23:00:00', price: 180000 },
                ],
            });
            setMsg({ type: 'success', text: 'Đã tạo sân con và sinh tự động 3 khung giờ thi đấu!' });
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const handleVerifyQr = async () => {
        if (!qrTokenInput) return;
        try {
            const res = await axiosClient.post(`/bookings/verify-qr?qrToken=${qrTokenInput}`);
            setBookingIdForComplete(res.data.id);
            setMsg({
                type: 'success',
                text: `Quét QR thành công (CHECKED_IN)! Vui lòng thu 50% còn lại tại sân: ${Number(
                    res.data.remainingAmount
                ).toLocaleString()} VNĐ`,
            });
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const handleCompleteBooking = async () => {
        if (!bookingIdForComplete) return;
        try {
            await axiosClient.post(`/bookings/${bookingIdForComplete}/complete`);
            setMsg({
                type: 'success',
                text: 'Đã bấm "Đã thu đủ"! Đơn chuyển sang COMPLETED, giải ngân tiền cọc & cộng +2đ uy tín cho khách.',
            });
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const handleMarkNoShow = async () => {
        if (!bookingIdForComplete) return;
        try {
            await axiosClient.post(`/bookings/${bookingIdForComplete}/no-show`);
            setMsg({
                type: 'error',
                text: 'Đã đánh dấu khách Vắng mặt (NO_SHOW)! Khách mất cọc và bị trừ 20 điểm uy tín.',
            });
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const handleCreateVoucher = async (e) => {
        e.preventDefault();
        try {
            const payload = {
                ...voucherForm,
                facilityId: voucherForm.issuerType === 'FACILITY' ? voucherForm.facilityId : null,
            };
            await axiosClient.post('/vouchers', payload);
            setMsg({ type: 'success', text: `Tạo mã giảm giá ${voucherForm.code} (${voucherForm.issuerType}) thành công!` });
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const handleLoadRevenue = async () => {
        if (!courtForm.facilityId) return;
        try {
            const res = await axiosClient.get(`/bookings/owner-revenue?facilityId=${courtForm.facilityId}`);
            setRevenueReport(res.data);
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

            <div className="grid lg:grid-cols-2 gap-6">
                {/* Khối 1: Đăng ký Cụm sân & Admin Phê duyệt */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 space-y-4">
                    <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                        <Building2 className="w-5 h-5 text-emerald-600" />
                        1. Đăng Ký Đối Tác Sân & Admin Phê Duyệt
                    </h2>
                    <form onSubmit={handleRegisterFacility} className="space-y-3 text-sm">
                        <input
                            type="text"
                            value={facForm.name}
                            onChange={(e) => setFacForm({ ...facForm, name: e.target.value })}
                            className="w-full border border-slate-300 rounded-lg px-3 py-2"
                            placeholder="Tên cụm sân"
                        />
                        <input
                            type="text"
                            value={facForm.address}
                            onChange={(e) => setFacForm({ ...facForm, address: e.target.value })}
                            className="w-full border border-slate-300 rounded-lg px-3 py-2"
                            placeholder="Địa chỉ cụm sân"
                        />
                        <button
                            type="submit"
                            className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-semibold py-2 rounded-lg cursor-pointer"
                        >
                            Chủ Sân: Gửi Hồ Sơ Đăng Ký Cụm Sân
                        </button>
                    </form>

                    <div className="border-t border-slate-200 pt-4 space-y-2 text-sm">
                        <label className="block font-bold text-slate-700 flex items-center gap-1">
                            <ShieldAlert className="w-4 h-4 text-amber-600" />
                            Dành cho ADMIN: Duyệt Hồ Sơ Cụm Sân
                        </label>
                        <input
                            type="text"
                            value={createdFacilityId}
                            onChange={(e) => setCreatedFacilityId(e.target.value)}
                            placeholder="Nhập Facility ID cần duyệt"
                            className="w-full border border-slate-300 rounded-lg px-3 py-2"
                        />
                        <div className="grid grid-cols-2 gap-2">
                            <button
                                onClick={() => handleAdminApprove('ACTIVE')}
                                className="bg-slate-900 text-white py-2 rounded-lg font-semibold cursor-pointer"
                            >
                                Phê duyệt (ACTIVE)
                            </button>
                            <button
                                onClick={() => handleAdminApprove('REJECTED')}
                                className="bg-red-600 text-white py-2 rounded-lg font-semibold cursor-pointer"
                            >
                                Từ chối (REJECTED)
                            </button>
                        </div>
                    </div>

                    <form onSubmit={handleCreateCourtAndSlots} className="border-t border-slate-200 pt-4 space-y-3 text-sm">
                        <h3 className="font-bold text-slate-700">2. Thêm Sân Con & Khung Giờ (Chủ Sân)</h3>
                        <input
                            type="text"
                            required
                            value={courtForm.facilityId}
                            onChange={(e) => setCourtForm({ ...courtForm, facilityId: e.target.value })}
                            placeholder="Facility ID (Đã được Admin duyệt ACTIVE)"
                            className="w-full border border-slate-300 rounded-lg px-3 py-2"
                        />
                        <div className="grid grid-cols-3 gap-2">
                            <input
                                type="text"
                                value={courtForm.courtName}
                                onChange={(e) => setCourtForm({ ...courtForm, courtName: e.target.value })}
                                className="border border-slate-300 rounded-lg px-2 py-2"
                            />
                            <select
                                value={courtForm.sportType}
                                onChange={(e) => setCourtForm({ ...courtForm, sportType: e.target.value })}
                                className="border border-slate-300 rounded-lg px-2 py-2 bg-white"
                            >
                                <option value="PICKLEBALL">PICKLEBALL</option>
                                <option value="BADMINTON">BADMINTON</option>
                                <option value="TABLE_TENNIS">TABLE_TENNIS</option>
                            </select>
                            <input
                                type="date"
                                value={courtForm.playDate}
                                onChange={(e) => setCourtForm({ ...courtForm, playDate: e.target.value })}
                                className="border border-slate-300 rounded-lg px-2 py-2"
                            />
                        </div>
                        <button
                            type="submit"
                            className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-semibold py-2 rounded-lg cursor-pointer"
                        >
                            Tạo Sân Con & Sinh Khung Giờ (17h-23h)
                        </button>
                    </form>
                </div>

                {/* Khối 2: Quét QR Check-in, Chốt doanh thu & Phát hành Voucher */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 space-y-4">
                    <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                        <QrCode className="w-5 h-5 text-emerald-600" />
                        3. Quét QR Check-in & Thu 50% Còn Lại Tại Sân
                    </h2>
                    <div className="space-y-3 text-sm">
                        <div className="flex gap-2">
                            <input
                                type="text"
                                value={qrTokenInput}
                                onChange={(e) => setQrTokenInput(e.target.value)}
                                placeholder="Dán mã QR Token của khách (VD: QR-8a7b...)"
                                className="flex-1 border border-slate-300 rounded-lg px-3 py-2"
                            />
                            <button
                                onClick={handleVerifyQr}
                                className="bg-emerald-600 text-white font-semibold px-4 py-2 rounded-lg cursor-pointer"
                            >
                                Quét QR
                            </button>
                        </div>

                        <div className="flex gap-2 items-center">
                            <input
                                type="text"
                                value={bookingIdForComplete}
                                onChange={(e) => setBookingIdForComplete(e.target.value)}
                                placeholder="Booking ID cần chốt hoàn tất hoặc báo No-show"
                                className="flex-1 border border-slate-300 rounded-lg px-3 py-2"
                            />
                            <button
                                onClick={handleCompleteBooking}
                                className="bg-slate-900 text-white text-xs font-bold px-3 py-2.5 rounded-lg cursor-pointer"
                            >
                                Đã thu đủ 50%
                            </button>
                            <button
                                onClick={handleMarkNoShow}
                                className="bg-red-600 text-white text-xs font-bold px-3 py-2.5 rounded-lg cursor-pointer"
                            >
                                Báo No-Show
                            </button>
                        </div>
                    </div>

                    {/* Tạo Voucher */}
                    <form onSubmit={handleCreateVoucher} className="border-t border-slate-200 pt-4 space-y-3 text-sm">
                        <h3 className="font-bold text-slate-700 flex items-center gap-1.5">
                            <TicketPercent className="w-4 h-4 text-emerald-600" />
                            4. Phát Hành Voucher (Chủ sân vs Admin Sàn)
                        </h3>
                        <div className="grid grid-cols-2 gap-2">
                            <input
                                type="text"
                                value={voucherForm.code}
                                onChange={(e) => setVoucherForm({ ...voucherForm, code: e.target.value })}
                                placeholder="Mã Voucher (VD: SANVIP20)"
                                className="border border-slate-300 rounded-lg px-3 py-2"
                            />
                            <select
                                value={voucherForm.issuerType}
                                onChange={(e) => setVoucherForm({ ...voucherForm, issuerType: e.target.value })}
                                className="border border-slate-300 rounded-lg px-3 py-2 bg-white"
                            >
                                <option value="FACILITY">FACILITY (Trừ doanh thu sân)</option>
                                <option value="PLATFORM">PLATFORM (Trừ phí sàn Admin)</option>
                            </select>
                        </div>
                        <button
                            type="submit"
                            className="w-full bg-amber-500 hover:bg-amber-600 text-white font-bold py-2 rounded-lg cursor-pointer"
                        >
                            Tạo Mã Giảm Giá
                        </button>
                    </form>

                    {/* Đối soát doanh thu */}
                    <div className="border-t border-slate-200 pt-4 space-y-2 text-sm">
                        <div className="flex justify-between items-center">
              <span className="font-bold text-slate-700 flex items-center gap-1.5">
                <BarChart3 className="w-4 h-4 text-emerald-600" />
                5. Báo Cáo Đối Soát Doanh Thu Cụm Sân
              </span>
                            <button
                                onClick={handleLoadRevenue}
                                className="text-xs bg-emerald-600 text-white px-3 py-1.5 rounded-lg font-semibold cursor-pointer"
                            >
                                Xem doanh thu
                            </button>
                        </div>
                        {revenueReport && (
                            <div className="bg-slate-50 p-3.5 rounded-xl border border-slate-200 text-xs space-y-1">
                                <p><strong>Số trận đã hoàn tất (COMPLETED):</strong> {revenueReport.completedBookingsCount} đơn</p>
                                <p><strong>Tổng doanh thu gốc (Gross):</strong> {Number(revenueReport.totalGrossRevenue).toLocaleString()} VNĐ</p>
                                <p className="text-emerald-700 font-bold">
                                    Tiền cọc thực nhận từ Hệ thống (Đã trừ phí sàn): {Number(revenueReport.totalNetDepositFromSystem).toLocaleString()} VNĐ
                                </p>
                                <p className="text-amber-700 font-bold">
                                    Tiền 50% còn lại Chủ sân tự thu tại sân: {Number(revenueReport.totalCollectedAtCourt).toLocaleString()} VNĐ
                                </p>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}