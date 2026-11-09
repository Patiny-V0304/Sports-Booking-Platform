import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Trophy, MapPin, Users, ShieldCheck, LogOut, UserCheck } from 'lucide-react';

export default function Navbar() {
    const { user, logout } = useAuth();
    const location = useLocation();

    const navClass = (path) =>
        `flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold transition ${
            location.pathname === path
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-slate-600 hover:bg-slate-100'
        }`;

    return (
        <header className="bg-white border-b border-slate-200 sticky top-0 z-50">
            <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
                <div className="flex items-center gap-8">
                    <Link to="/" className="flex items-center gap-2 text-emerald-600 font-bold text-xl">
                        <Trophy className="w-7 h-7" />
                        <span>SportMatch</span>
                    </Link>

                    <nav className="hidden md:flex items-center gap-2">
                        <Link to="/" className={navClass('/')}>
                            <MapPin className="w-4 h-4" />
                            Đặt Sân & Cọc 50%
                        </Link>
                        <Link to="/matchmaking" className={navClass('/matchmaking')}>
                            <Users className="w-4 h-4" />
                            Ghép Kèo & Đánh Giá
                        </Link>
                        <Link to="/management" className={navClass('/management')}>
                            <ShieldCheck className="w-4 h-4" />
                            Quản Lý Sân & Admin
                        </Link>
                    </nav>
                </div>

                <div className="flex items-center gap-3">
                    {user ? (
                        <>
                            <Link
                                to="/auth"
                                className="flex items-center gap-2 bg-slate-100 hover:bg-slate-200 px-3 py-1.5 rounded-lg text-sm"
                            >
                                <UserCheck className="w-4 h-4 text-emerald-600" />
                                <span className="font-semibold text-slate-800">{user.fullName}</span>
                                <span className="text-xs px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 font-bold">
                  {user.role}
                </span>
                                <span className="text-xs px-2 py-0.5 rounded bg-amber-100 text-amber-800 font-semibold">
                  Uy tín: {user.reliabilityScore ?? 100}đ
                </span>
                            </Link>
                            <button
                                onClick={logout}
                                className="p-2 text-slate-500 hover:text-red-600 hover:bg-red-50 rounded-lg cursor-pointer"
                                title="Đăng xuất"
                            >
                                <LogOut className="w-5 h-5" />
                            </button>
                        </>
                    ) : (
                        <Link
                            to="/auth"
                            className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-lg text-sm font-semibold"
                        >
                            Đăng nhập / Đăng ký
                        </Link>
                    )}
                </div>
            </div>
        </header>
    );
}