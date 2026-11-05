import { createContext, useContext, useEffect, useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from './auth'
import { get } from './api'

/* ---------------- Toasts (same behaviour as the legacy #toast element) ---------------- */
const ToastContext = createContext(() => {})

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null)
  useEffect(() => {
    if (!toast) return
    const t = setTimeout(() => setToast(null), 3200)
    return () => clearTimeout(t)
  }, [toast])
  return (
    <ToastContext.Provider value={setToast}>
      {children}
      {toast && (
        <div className="toast fixed right-6 bottom-6 z-50 rounded-[10px] bg-ink px-[18px] py-3 text-sm text-white shadow-card opacity-100">
          {toast.icon && <span className="mr-2">{toast.icon}</span>}{toast.text || toast}
        </div>
      )}
    </ToastContext.Provider>
  )
}
export const useToast = () => useContext(ToastContext)

/* ---------------- header / footer shell ---------------- */
export function Layout({ children }) {
  const { user, isAdmin, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const [unread, setUnread] = useState(0)

  useEffect(() => {
    if (!user) return setUnread(0)
    let alive = true
    const load = () => get('/api/notifications/unread-count')
      .then((n) => alive && setUnread(n))
      .catch(() => {})
    load()
    const timer = setInterval(load, 30000) // F12: keeps the header badge fresh
    return () => { alive = false; clearInterval(timer) }
  }, [user])

  const navClass = ({ isActive }) =>
    `text-[13px] font-semibold no-underline ${isActive ? 'text-brand' : 'text-[#58657b] hover:text-brand'}`

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-20 h-[76px] border-b border-line bg-white/95 backdrop-blur">
        <div className="mx-auto flex h-full max-w-[1160px] items-center justify-between gap-6 px-6">
          <Link to="/" className="flex items-center gap-2.5 font-display text-xl font-extrabold text-ink">
            <span className="grid h-[34px] w-[34px] place-items-center rounded-[11px] bg-gradient-to-br from-[#4268ef] to-[#263fae] font-bold text-white">S</span>
            SkillBridge
          </Link>
          <button className="text-2xl md:hidden" onClick={() => setMenuOpen(!menuOpen)} aria-label="Toggle navigation">☰</button>
          <nav className={`${menuOpen ? 'flex' : 'hidden'} md:flex flex-col md:flex-row gap-5 md:gap-6 ${''}`}>
            <NavLink to="/" end className={navClass} onClick={() => setMenuOpen(false)}>Home</NavLink>
            {user && <>
              <NavLink to="/dashboard" className={navClass} onClick={() => setMenuOpen(false)}>Dashboard</NavLink>
              <NavLink to="/projects" className={navClass} onClick={() => setMenuOpen(false)}>Projects</NavLink>
              <NavLink to="/teammates" className={navClass} onClick={() => setMenuOpen(false)}>Find teammates</NavLink>
              <NavLink to="/tasks" className={navClass} onClick={() => setMenuOpen(false)}>Tasks</NavLink>
              <NavLink to="/mentorship" className={navClass} onClick={() => setMenuOpen(false)}>Mentorship</NavLink>
            </>}
            {isAdmin && <NavLink to="/admin" className={`${navClass} text-teal`} onClick={() => setMenuOpen(false)}>Admin</NavLink>}
          </nav>
          <div className="hidden items-center gap-4 md:flex">
            {user ? (
              <>
                <Link to="/notifications" className="relative text-[13px] font-semibold text-[#58657b] hover:text-brand">
                  🔔{unread > 0 && <span className="absolute -right-3 -top-2 rounded-full bg-brand px-1.5 text-[10px] font-bold text-white">{unread}</span>}
                </Link>
                <Link to="/profile" className="text-[13px] font-semibold text-[#58657b] hover:text-brand">{user.fullName.split(' ')[0]}</Link>
                <button className="btn-secondary btn-sm" onClick={async () => { await logout(); navigate('/') }}>Log out</button>
              </>
            ) : (
              <>
                <Link to="/login" className="text-[13px] font-semibold text-[#58657b] hover:text-brand">Log in</Link>
                <Link to="/register" className="btn-primary btn-sm">Join now</Link>
              </>
            )}
          </div>
        </div>
      </header>
      <main className="mx-auto min-h-[calc(100vh-145px)] w-[min(1160px,90%)] pb-16 pt-10">
        {children}
      </main>
      <footer className="flex justify-between border-t border-line bg-white px-[5%] py-5 text-xs text-[#8993a5]">
        <span>© 2026 SkillBridge</span><span>Learn · Collaborate · Build</span>
      </footer>
    </div>
  )
}

/* ---------------- small shared pieces ---------------- */
export function PageHeading({ title, lead, action }) {
  return (
    <div className="mb-7 flex flex-wrap items-end justify-between gap-5">
      <div>
        <h1 className="h-display text-[clamp(27px,4vw,36px)]">{title}</h1>
        {lead && <p className="m-0 text-muted">{lead}</p>}
      </div>
      {action}
    </div>
  )
}

export function Panel({ title, children, className = '' }) {
  return (
    <section className={`panel ${className}`}>
      {title && <h2 className="panel-title mb-4 text-lg">{title}</h2>}
      {children}
    </section>
  )
}

export function EmptyState({ icon = '🗂️', text }) {
  return (
    <div className="empty-state py-9 text-center text-muted">
      <div className="mb-2 text-3xl">{icon}</div>
      {text}
    </div>
  )
}

export function StatusBadge({ value }) {
  const map = {
    PUBLISHED: 'badge-green', DRAFT: 'badge-amber', PAUSED: 'badge-amber',
    COMPLETED: 'badge-sky', CANCELLED: 'badge-red',
    TODO: 'badge-sky', IN_PROGRESS: 'badge-amber', BLOCKED: 'badge-red',
    READY_FOR_REVIEW: 'badge-amber', APPROVED: 'badge-green', REOPENED: 'badge-red',
    LOW: 'badge-sky', MEDIUM: 'badge-amber', HIGH: 'badge-red',
    PENDING: 'badge-amber', ACCEPTED: 'badge-green', REJECTED: 'badge-red', CANCELLED2: 'badge-red',
    OPEN: 'badge-green', IN_PROGRESS2: 'badge-amber', CLOSED: 'badge-sky', DECLINED: 'badge-red',
    PUBLIC: 'badge-sky', PRIVATE: 'badge-amber', ACTIVE: 'badge-green', SUSPENDED: 'badge-red',
    MEMBER: 'badge-sky', MANAGER: 'badge-amber', OWNER: 'badge-green', ADMIN: 'badge-red',
    BEGINNER: 'badge-sky', INTERMEDIATE: 'badge-amber', ADVANCED: 'badge-green', EXPERT: 'badge-red',
  }
  const key = value || 'badge-sky'
  return <span className={`badge ${map[key] || key.replace('_2', '') || 'badge-sky'}`}>{String(value || '—').replace(/_/g, ' ')}</span>
}

export function Field({ label, hint, children }) {
  return (
    <label className="mb-4 flex flex-col gap-2">
      <span className="field-label">{label}</span>
      {children}
      {hint && <span className="text-[11px] text-[#8993a5]">{hint}</span>}
    </label>
  )
}

export function Spinner({ className = '' }) {
  return <div className={`animate-pulse text-muted ${className}`}>Loading…</div>
}

export function AvatarStack({ names = [] }) {
  return (
    <div className="flex -space-x-2">
      {names.slice(0, 3).map((n, i) => (
        <span key={i} className="grid h-7 w-7 place-items-center rounded-full border-2 border-white bg-soft text-[10px] font-bold text-brand">
          {n.split(' ').map(p => p[0]).join('').slice(0, 2)}
        </span>
      ))}
    </div>
  )
}
