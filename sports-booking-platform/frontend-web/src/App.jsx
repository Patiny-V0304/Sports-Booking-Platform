import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Navbar from './components/Navbar';
import BookingPage from './pages/BookingPage';
import MatchmakingPage from './pages/MatchmakingPage';
import ManagementPage from './pages/ManagementPage';
import AuthPage from './pages/AuthPage';

export default function App() {
  return (
      <AuthProvider>
        <BrowserRouter>
          <div className="min-h-screen bg-slate-100 text-slate-900">
            <Navbar />
            <main className="py-4">
              <Routes>
                <Route path="/" element={<BookingPage />} />
                <Route path="/matchmaking" element={<MatchmakingPage />} />
                <Route path="/management" element={<ManagementPage />} />
                <Route path="/auth" element={<AuthPage />} />
              </Routes>
            </main>
          </div>
        </BrowserRouter>
      </AuthProvider>
  );
}