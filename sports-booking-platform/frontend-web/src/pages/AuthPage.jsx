import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import axiosClient from '../api/axiosClient';

export default function AuthPage() {
    const { user, login, register } = useAuth();
    const [isLogin, setIsLogin] = useState(true);
    const [msg, setMsg] = useState({ type: '', text: '' });
    const [profileDetail, setProfileDetail] = useState(null);

    const [loginForm, setLoginForm] = useState({ phoneOrEmail: '', password: '' });
    const [regForm, setRegForm] = useState({
        phone: '',
        email: '',
        password: '',
        fullName: '',
        role: 'PLAYER',
    });

    const [sportForm, setSportForm] = useState({
        sportType: 'PICKLEBALL',
        skillLevel: 'INTERMEDIATE',
    });

    const [vietQrForm, setVietQrForm] = useState({
        bankName: 'MBBank',
        bankAccountNo: '0901234567',
        vietqrImageUrl: 'https://img.vietqr.io/image/MB-0901234567-compact2.png',
    });

    const fetchMyProfile = async () => {
        if (!user) return;
        try {
            const res = await axiosClient.get('/users/me');
            setProfileDetail(res.data);
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    useEffect(() => {
        fetchMyProfile();
    }, [user]);

    const handleAuthSubmit = async (e) => {
        e.preventDefault();
        setMsg({ type: '', text: '' });
        try {
            if (isLogin) {
                await login(loginForm.phoneOrEmail, loginForm.password);
                setMsg({ type: 'success', text: 'Đăng nhập thành công!' });
            } else {
                await register(regForm);
                setMsg({ type: 'success', text: 'Đăng ký tài khoản thành công!' });
            }
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const handleUpdateSport = async (e) => {
        e.preventDefault();
        try {
            await axiosClient.put('/profiles/sports', sportForm);
            setMsg({ type: 'success', text: 'Đã cập nhật trình độ thể thao!' });
            fetchMyProfile();
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    const handleUpdateVietQr = async (e) => {
        e.preventDefault();
        try {
            await axiosClient.put('/users/me/vietqr', vietQrForm);
            setMsg({ type: 'success', text: 'Đã lưu thông tin VietQR nhận tiền chia sẻ!' });
            fetchMyProfile();
        } catch (err) {
            setMsg({ type: 'error', text: err.message });
        }
    };

    return (
        <div className="max-w-5xl mx-auto p-6 space-y-6">
            {msg.text && (
                <div
                    className={`p-4 rounded-xl font-medium ${
                        msg.type === 'error' ? 'bg-red-50 text-red-700 border border-red-200' : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    }`}
                >
                    {msg.text}
                </div>
            )}

            <div className="grid md:grid-cols-2 gap-6">
                {/* Cột 1: Đăng nhập / Đăng ký */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                    <div className="flex gap-2 mb-6 bg-slate-100 p-1 rounded-xl">
                        <button
                            onClick={() => setIsLogin(true)}
                            className={`flex-1 py-2 rounded-lg text-sm font-semibold cursor-pointer ${
                                isLogin ? 'bg-white shadow text-emerald-600' : 'text-slate-600'
                            }`}
                        >
                            Đăng nhập
                        </button>
                        <button
                            onClick={() => setIsLogin(false)}
                            className={`flex-1 py-2 rounded-lg text-sm font-semibold cursor-pointer ${
                                !isLogin ? 'bg-white shadow text-emerald-600' : 'text-slate-600'
                            }`}
                        >
                            Đăng ký tài khoản mới
                        </button>
                    </div>

                    <form onSubmit={handleAuthSubmit} className="space-y-4">
                        {isLogin ? (
                            <>
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">Số điện thoại hoặc Email</label>
                                    <input
                                        type="text"
                                        required
                                        value={loginForm.phoneOrEmail}
                                        onChange={(e) => setLoginForm({ ...loginForm, phoneOrEmail: e.target.value })}
                                        className="w-full border border-slate-300 rounded-lg px-3 py-2"
                                        placeholder="0901234567 hoặc email@gmail.com"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">Mật khẩu</label>
                                    <input
                                        type="password"
                                        required
                                        value={loginForm.password}
                                        onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })}
                                        className="w-full border border-slate-300 rounded-lg px-3 py-2"
                                    />
                                </div>
                            </>
                        ) : (
                            <>
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">Họ và tên</label>
                                    <input
                                        type="text"
                                        required
                                        value={regForm.fullName}
                                        onChange={(e) => setRegForm({ ...regForm, fullName: e.target.value })}
                                        className="w-full border border-slate-300 rounded-lg px-3 py-2"
                                        placeholder="Nguyễn Văn A"
                                    />
                                </div>
                                <div className="grid grid-cols-2 gap-3">
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">Số điện thoại</label>
                                        <input
                                            type="text"
                                            required
                                            value={regForm.phone}
                                            onChange={(e) => setRegForm({ ...regForm, phone: e.target.value })}
                                            className="w-full border border-slate-300 rounded-lg px-3 py-2"
                                            placeholder="0901234567"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">Vai trò (Role)</label>
                                        <select
                                            value={regForm.role}
                                            onChange={(e) => setRegForm({ ...regForm, role: e.target.value })}
                                            className="w-full border border-slate-300 rounded-lg px-3 py-2 bg-white"
                                        >
                                            <option value="PLAYER">Người chơi (PLAYER)</option>
                                            <option value="FACILITY_MANAGER">Chủ sân (FACILITY_MANAGER)</option>
                                            <option value="ADMIN">Quản trị viên (ADMIN)</option>
                                        </select>
                                    </div>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">Email</label>
                                    <input
                                        type="email"
                                        required
                                        value={regForm.email}
                                        onChange={(e) => setRegForm({ ...regForm, email: e.target.value })}
                                        className="w-full border border-slate-300 rounded-lg px-3 py-2"
                                        placeholder="user@gmail.com"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">Mật khẩu</label>
                                    <input
                                        type="password"
                                        required
                                        value={regForm.password}
                                        onChange={(e) => setRegForm({ ...regForm, password: e.target.value })}
                                        className="w-full border border-slate-300 rounded-lg px-3 py-2"
                                    />
                                </div>
                            </>
                        )}

                        <button
                            type="submit"
                            className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-semibold py-2.5 rounded-lg cursor-pointer"
                        >
                            {isLogin ? 'Đăng nhập vào hệ thống' : 'Tạo tài khoản mới'}
                        </button>
                    </form>
                </div>

                {/* Cột 2: Hồ sơ thể thao & Cấu hình VietQR */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 space-y-6">
                    <h2 className="text-lg font-bold text-slate-800">Hồ Sơ Cá Nhân & VietQR</h2>

                    {!user ? (
                        <p className="text-slate-500 text-sm">Vui lòng đăng nhập để xem hồ sơ và cấu hình trình độ thể thao.</p>
                    ) : (
                        <>
                            <div className="bg-slate-50 p-4 rounded-xl text-sm space-y-1 border border-slate-200">
                                <p><strong>User ID (UUID):</strong> <code className="text-xs bg-slate-200 px-1.5 py-0.5 rounded">{user.userId}</code></p>
                                <p><strong>Họ tên:</strong> {profileDetail?.fullName || user.fullName}</p>
                                <p><strong>Điểm uy tín hiện tại:</strong> <span className="text-emerald-700 font-bold">{profileDetail?.reliabilityScore ?? 100} / 100</span></p>
                                {profileDetail?.sportProfiles?.length > 0 && (
                                    <div className="pt-2">
                                        <strong>Trình độ đã khai báo:</strong>
                                        <div className="flex flex-wrap gap-2 mt-1">
                                            {profileDetail.sportProfiles.map((sp) => (
                                                <span key={sp.id} className="bg-emerald-100 text-emerald-800 text-xs font-semibold px-2.5 py-1 rounded">
                          {sp.sportType}: {sp.skillLevel}
                        </span>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>

                            <form onSubmit={handleUpdateSport} className="space-y-3 border-t border-slate-200 pt-4">
                                <h3 className="text-sm font-bold text-slate-700">1. Cập nhật trình độ thể thao (3 môn cố định)</h3>
                                <div className="grid grid-cols-2 gap-3">
                                    <select
                                        value={sportForm.sportType}
                                        onChange={(e) => setSportForm({ ...sportForm, sportType: e.target.value })}
                                        className="border border-slate-300 rounded-lg px-3 py-2 text-sm bg-white"
                                    >
                                        <option value="BADMINTON">Cầu lông (BADMINTON)</option>
                                        <option value="PICKLEBALL">Pickleball (PICKLEBALL)</option>
                                        <option value="TABLE_TENNIS">Bóng bàn (TABLE_TENNIS)</option>
                                    </select>
                                    <select
                                        value={sportForm.skillLevel}
                                        onChange={(e) => setSportForm({ ...sportForm, skillLevel: e.target.value })}
                                        className="border border-slate-300 rounded-lg px-3 py-2 text-sm bg-white"
                                    >
                                        <option value="NEWBIE">Mới chơi (NEWBIE)</option>
                                        <option value="INTERMEDIATE">Trung bình (INTERMEDIATE)</option>
                                        <option value="GOOD">Khá (GOOD)</option>
                                        <option value="EXCELLENT">Tốt (EXCELLENT)</option>
                                    </select>
                                </div>
                                <button type="submit" className="bg-slate-800 hover:bg-slate-900 text-white text-sm font-medium px-4 py-2 rounded-lg cursor-pointer">
                                    Lưu trình độ
                                </button>
                            </form>

                            <form onSubmit={handleUpdateVietQr} className="space-y-3 border-t border-slate-200 pt-4">
                                <h3 className="text-sm font-bold text-slate-700">2. Cấu hình VietQR nhận tiền chia sẻ (Dành cho Host)</h3>
                                <div className="grid grid-cols-2 gap-3">
                                    <input
                                        type="text"
                                        value={vietQrForm.bankName}
                                        onChange={(e) => setVietQrForm({ ...vietQrForm, bankName: e.target.value })}
                                        className="border border-slate-300 rounded-lg px-3 py-2 text-sm"
                                        placeholder="Tên ngân hàng (VD: MBBank)"
                                    />
                                    <input
                                        type="text"
                                        value={vietQrForm.bankAccountNo}
                                        onChange={(e) => {
                                            const acc = e.target.value;
                                            setVietQrForm({
                                                ...vietQrForm,
                                                bankAccountNo: acc,
                                                vietqrImageUrl: `https://img.vietqr.io/image/MB-${acc}-compact2.png`,
                                            });
                                        }}
                                        className="border border-slate-300 rounded-lg px-3 py-2 text-sm"
                                        placeholder="Số tài khoản"
                                    />
                                </div>
                                <button type="submit" className="bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-medium px-4 py-2 rounded-lg cursor-pointer">
                                    Lưu mã VietQR cá nhân
                                </button>
                            </form>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}