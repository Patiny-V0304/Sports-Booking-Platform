import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';
import { Users, QrCode, Star, Check, Send } from 'lucide-react';

export default function MatchmakingPage() {
    const [lobbies, setLobbies] = useState([]);
    const [msg, setMsg] = useState({ type: '', text: '' });

    const [createForm, setCreateForm] = useState({
        bookingId: '',
        sportType: 'PICKLEBALL',
        maxRecruits: 3,
        requiredSkillLevel: 'INTERMEDIATE',
        shareAmountPerPerson: 50000,
        hostVietqrUrl: 'https://img.vietqr.io/image/MB-0901234567-compact2.png',
    });

    const [approvalMemberId, setApprovalMemberId] = useState('');
    const [selectedLobbyForAction, setSelectedLobbyForAction] = useState(null);

    const [reviewForm, setReviewForm] = useState({
        bookingId: '',
        lobbyId: '',
        starRating: 5,
        comment: 'Mặt sân chuẩn thi đấu, đèn sáng không bị chói!',
        targetUserId: '',
        votedSkillLevel: 'GOOD',
        isNoShow: false,
    });

    const loadLobbies = async () => {
        try {
            const res = await axiosClient.get('/lobbies');
            setLobbies(res.data || []);
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    useEffect(() => {
        loadLobbies();
    }, []);

    const handleCreateLobby = async (e) => {
        e.preventDefault();
        try {
            const res = await axiosClient.post('/lobbies', createForm);
            setMsg({ type: 'success', text: `Mở phòng ghép kèo thành công! Lobby ID: ${res.data.id}` });
            setSelectedLobbyForAction(res.data);
            loadLobbies();
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const handleJoinLobby = async (lobbyId) => {
        try {
            await axiosClient.post(`/lobbies/${lobbyId}/join`);
            setMsg({ type: 'success', text: 'Đã gửi yêu cầu xin tham gia phòng! Chờ Chủ phòng duyệt.' });
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const handleApproveMember = async (lobbyId, decision) => {
        if (!approvalMemberId) return;
        try {
            await axiosClient.put(
                `/lobbies/${lobbyId}/members/${approvalMemberId}/approval?decision=${decision}`
            );
            setMsg({ type: 'success', text: `Đã xử lý thành viên (${decision})! Mã VietQR của Host đã mở.` });
            loadLobbies();
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const handleReportPaid = async (lobbyId) => {
        try {
            await axiosClient.post(`/lobbies/${lobbyId}/report-paid`);
            setMsg({ type: 'success', text: 'Đã báo chuyển khoản (REPORTED_PAID) cho Chủ phòng!' });
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const handleConfirmPaid = async (lobbyId) => {
        if (!approvalMemberId) return;
        try {
            await axiosClient.put(`/lobbies/${lobbyId}/members/${approvalMemberId}/confirm-paid`);
            setMsg({ type: 'success', text: 'Chủ phòng đã xác nhận nhận đủ tiền chia sẻ (PAID)!' });
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const handleSubmitReview = async (e) => {
        e.preventDefault();
        try {
            const payload = {
                bookingId: reviewForm.bookingId,
                lobbyId: reviewForm.lobbyId || null,
                facilityReview: {
                    starRating: Number(reviewForm.starRating),
                    comment: reviewForm.comment,
                },
                anonymousPlayerReviews: reviewForm.targetUserId
                    ? [
                        {
                            targetUserId: reviewForm.targetUserId,
                            votedSkillLevel: reviewForm.votedSkillLevel,
                            isNoShow: reviewForm.isNoShow,
                        },
                    ]
                    : [],
            };
            await axiosClient.post('/reviews/post-match', payload);
            setMsg({
                type: 'success',
                text: 'Đã gửi đánh giá sao sân & Đánh giá ẩn danh đồng đội (Mã hóa SHA-256)!',
            });
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
                {/* Cột 1: Mở phòng ghép kèo */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 space-y-4">
                    <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                        <Users className="w-5 h-5 text-emerald-600" />
                        1. Mở Phòng Tuyển Đồng Đội (Host)
                    </h2>
                    <form onSubmit={handleCreateLobby} className="space-y-3 text-sm">
                        <div>
                            <label className="block font-medium text-slate-700 mb-1">Booking ID (Đơn đã BOOKED)</label>
                            <input
                                type="text"
                                required
                                value={createForm.bookingId}
                                onChange={(e) => setCreateForm({ ...createForm, bookingId: e.target.value })}
                                placeholder="Dán UUID của đơn đặt sân BOOKED"
                                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                            />
                        </div>
                        <div className="grid grid-cols-2 gap-2">
                            <div>
                                <label className="block font-medium text-slate-700 mb-1">Môn thể thao</label>
                                <select
                                    value={createForm.sportType}
                                    onChange={(e) => setCreateForm({ ...createForm, sportType: e.target.value })}
                                    className="w-full border border-slate-300 rounded-lg px-2 py-2 bg-white"
                                >
                                    <option value="PICKLEBALL">PICKLEBALL</option>
                                    <option value="BADMINTON">BADMINTON</option>
                                    <option value="TABLE_TENNIS">TABLE_TENNIS</option>
                                </select>
                            </div>
                            <div>
                                <label className="block font-medium text-slate-700 mb-1">Trình độ yêu cầu</label>
                                <select
                                    value={createForm.requiredSkillLevel}
                                    onChange={(e) => setCreateForm({ ...createForm, requiredSkillLevel: e.target.value })}
                                    className="w-full border border-slate-300 rounded-lg px-2 py-2 bg-white"
                                >
                                    <option value="NEWBIE">NEWBIE (Mới)</option>
                                    <option value="INTERMEDIATE">INTERMEDIATE (TB)</option>
                                    <option value="GOOD">GOOD (Khá)</option>
                                    <option value="EXCELLENT">EXCELLENT (Tốt)</option>
                                </select>
                            </div>
                        </div>
                        <div className="grid grid-cols-2 gap-2">
                            <div>
                                <label className="block font-medium text-slate-700 mb-1">Số người tuyển</label>
                                <input
                                    type="number"
                                    min="1"
                                    value={createForm.maxRecruits}
                                    onChange={(e) => setCreateForm({ ...createForm, maxRecruits: Number(e.target.value) })}
                                    className="w-full border border-slate-300 rounded-lg px-3 py-2"
                                />
                            </div>
                            <div>
                                <label className="block font-medium text-slate-700 mb-1">Tiền chia sẻ / người</label>
                                <input
                                    type="number"
                                    value={createForm.shareAmountPerPerson}
                                    onChange={(e) => setCreateForm({ ...createForm, shareAmountPerPerson: Number(e.target.value) })}
                                    className="w-full border border-slate-300 rounded-lg px-3 py-2"
                                />
                            </div>
                        </div>
                        <button
                            type="submit"
                            className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-semibold py-2.5 rounded-lg cursor-pointer"
                        >
                            Đăng Phòng Ghép Kèo
                        </button>
                    </form>
                </div>

                {/* Cột 2: Danh sách phòng đang mở & Chuyển khoản VietQR P2P */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 space-y-4">
                    <div className="flex justify-between items-center">
                        <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <QrCode className="w-5 h-5 text-emerald-600" />
                            2. Phòng Đang Mở & VietQR P2P
                        </h2>
                        <button onClick={loadLobbies} className="text-xs bg-slate-100 px-2.5 py-1 rounded font-medium cursor-pointer">
                            Tải lại
                        </button>
                    </div>

                    <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
                        {lobbies.map((lobby) => (
                            <div
                                key={lobby.id}
                                onClick={() => setSelectedLobbyForAction(lobby)}
                                className={`p-3.5 rounded-xl border text-xs space-y-2 cursor-pointer ${
                                    selectedLobbyForAction?.id === lobby.id ? 'border-emerald-600 bg-emerald-50/40' : 'border-slate-200'
                                }`}
                            >
                                <div className="flex justify-between font-bold text-sm">
                                    <span>{lobby.sportType} ({lobby.requiredSkillLevel})</span>
                                    <span className="text-emerald-700">
                    Tuyển: {lobby.currentRecruits}/{lobby.maxRecruits} ({lobby.status})
                  </span>
                                </div>
                                <p><strong>Lobby ID:</strong> <code className="bg-slate-100 px-1">{lobby.id}</code></p>
                                <p><strong>Giờ chơi:</strong> {lobby.startTime} - {lobby.endTime} ({lobby.playDate})</p>
                                <p><strong>Tiền chia sẻ:</strong> <span className="font-bold text-amber-700">{Number(lobby.shareAmountPerPerson).toLocaleString()} đ/người</span></p>

                                <div className="flex gap-2 pt-1">
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleJoinLobby(lobby.id);
                                        }}
                                        className="bg-emerald-600 text-white px-3 py-1.5 rounded-lg font-semibold cursor-pointer"
                                    >
                                        Xin vào đội (Member)
                                    </button>
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleReportPaid(lobby.id);
                                        }}
                                        className="bg-blue-600 text-white px-3 py-1.5 rounded-lg font-semibold cursor-pointer"
                                    >
                                        Báo đã CK VietQR
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>

                    {selectedLobbyForAction && (
                        <div className="border-t border-slate-200 pt-4 space-y-3 text-xs">
                            <p className="font-bold text-slate-700">Thao tác của Chủ phòng (Host) cho Lobby đã chọn:</p>
                            <img
                                src={`${selectedLobbyForAction.hostVietqrUrl}?amount=${selectedLobbyForAction.shareAmountPerPerson}`}
                                alt="VietQR Host"
                                className="w-36 mx-auto rounded-lg border border-slate-200"
                            />
                            <input
                                type="text"
                                value={approvalMemberId}
                                onChange={(e) => setApprovalMemberId(e.target.value)}
                                placeholder="Nhập User ID của Member cần Duyệt / Xác nhận tiền"
                                className="w-full border border-slate-300 rounded-lg px-2.5 py-1.5"
                            />
                            <div className="grid grid-cols-2 gap-2">
                                <button
                                    onClick={() => handleApproveMember(selectedLobbyForAction.id, 'APPROVED')}
                                    className="bg-slate-800 text-white py-2 rounded-lg font-semibold flex items-center justify-center gap-1 cursor-pointer"
                                >
                                    <Check className="w-3.5 h-3.5" /> Duyệt vào đội
                                </button>
                                <button
                                    onClick={() => handleConfirmPaid(selectedLobbyForAction.id)}
                                    className="bg-emerald-600 text-white py-2 rounded-lg font-semibold flex items-center justify-center gap-1 cursor-pointer"
                                >
                                    <Check className="w-3.5 h-3.5" /> Đã nhận tiền
                                </button>
                            </div>
                        </div>
                    )}
                </div>

                {/* Cột 3: Đánh giá sân & Đánh giá ẩn danh thành viên (SHA-256) */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 space-y-4">
                    <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                        <Star className="w-5 h-5 text-amber-500" />
                        3. Đánh Giá Sân & Ẩn Danh Đồng Đội
                    </h2>
                    <form onSubmit={handleSubmitReview} className="space-y-3 text-sm">
                        <div>
                            <label className="block font-medium text-slate-700 mb-1">Booking ID (Đã COMPLETED)</label>
                            <input
                                type="text"
                                required
                                value={reviewForm.bookingId}
                                onChange={(e) => setReviewForm({ ...reviewForm, bookingId: e.target.value })}
                                placeholder="UUID đơn đặt sân đã hoàn tất"
                                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                            />
                        </div>
                        <div className="grid grid-cols-2 gap-2">
                            <div>
                                <label className="block font-medium text-slate-700 mb-1">Điểm sao sân (1-5)</label>
                                <input
                                    type="number"
                                    min="1"
                                    max="5"
                                    value={reviewForm.starRating}
                                    onChange={(e) => setReviewForm({ ...reviewForm, starRating: e.target.value })}
                                    className="w-full border border-slate-300 rounded-lg px-3 py-2"
                                />
                            </div>
                            <div>
                                <label className="block font-medium text-slate-700 mb-1">Lobby ID (Nếu ghép đội)</label>
                                <input
                                    type="text"
                                    value={reviewForm.lobbyId}
                                    onChange={(e) => setReviewForm({ ...reviewForm, lobbyId: e.target.value })}
                                    placeholder="UUID phòng ghép kèo"
                                    className="w-full border border-slate-300 rounded-lg px-3 py-2"
                                />
                            </div>
                        </div>
                        <div>
                            <label className="block font-medium text-slate-700 mb-1">Nhận xét cụm sân</label>
                            <textarea
                                rows="2"
                                value={reviewForm.comment}
                                onChange={(e) => setReviewForm({ ...reviewForm, comment: e.target.value })}
                                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                            />
                        </div>

                        <div className="border-t border-slate-200 pt-3 space-y-2">
                            <p className="text-xs font-bold text-slate-700">
                                Đánh giá ẩn danh đồng đội (Băm SHA-256 chống lộ danh tính):
                            </p>
                            <input
                                type="text"
                                value={reviewForm.targetUserId}
                                onChange={(e) => setReviewForm({ ...reviewForm, targetUserId: e.target.value })}
                                placeholder="User ID của đồng đội được chấm"
                                className="w-full border border-slate-300 rounded-lg px-3 py-2 text-xs"
                            />
                            <div className="grid grid-cols-2 gap-2">
                                <select
                                    value={reviewForm.votedSkillLevel}
                                    onChange={(e) => setReviewForm({ ...reviewForm, votedSkillLevel: e.target.value })}
                                    className="border border-slate-300 rounded-lg px-2 py-1.5 text-xs bg-white"
                                >
                                    <option value="NEWBIE">Mới (NEWBIE)</option>
                                    <option value="INTERMEDIATE">Trung bình (INTERMEDIATE)</option>
                                    <option value="GOOD">Khá (GOOD)</option>
                                    <option value="EXCELLENT">Tốt (EXCELLENT)</option>
                                </select>
                                <label className="flex items-center gap-1.5 text-xs text-red-700 font-semibold">
                                    <input
                                        type="checkbox"
                                        checked={reviewForm.isNoShow}
                                        onChange={(e) => setReviewForm({ ...reviewForm, isNoShow: e.target.checked })}
                                    />
                                    Báo vắng mặt (-15đ uy tín)
                                </label>
                            </div>
                        </div>

                        <button
                            type="submit"
                            className="w-full bg-amber-500 hover:bg-amber-600 text-white font-bold py-2.5 rounded-lg flex items-center justify-center gap-2 cursor-pointer"
                        >
                            <Send className="w-4 h-4" /> Gửi Đánh Giá Sau Trận
                        </button>
                    </form>
                </div>
            </div>
        </div>
    );
}