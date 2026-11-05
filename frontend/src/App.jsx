import { useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { Layout } from './components'
import { useAuth } from './auth'
import * as pages from './pages'

export default function App() {
  const { booting, user, isAdmin } = useAuth()

  if (booting) {
    return <div className="grid min-h-screen place-items-center text-muted">Loading SkillBridge…</div>
  }

  return (
    <Layout>
      <Routes>
        <Route path="/" element={<pages.Home />} />
        <Route path="/login" element={user ? <Navigate to="/dashboard" /> : <pages.Login />} />
        <Route path="/register" element={user ? <Navigate to="/dashboard" /> : <pages.Register />} />
        <Route path="/forgot-password" element={<pages.ForgotPassword />} />
        <Route path="/reset-password" element={pages.ResetPassword ? <pages.ResetPassword /> : null} />
        <Route path="/change-password" element={<pages.ChangePassword />} />

        <Route path="/dashboard" element={<pages.Dashboard />} />
        <Route path="/projects" element={<pages.Projects />} />
        <Route path="/projects/:id" element={<pages.ProjectDetails />} />
        <Route path="/create-project" element={<pages.CreateProject />} />
        <Route path="/edit-project/:id" element={<pages.CreateProject editMode />} />
        <Route path="/requests" element={<pages.Requests />} />
        <Route path="/tasks" element={<pages.Tasks />} />
        <Route path="/teammates" element={<pages.Teammates />} />
        <Route path="/recommendations" element={pages.Recommendations ? <pages.Recommendations /> : null} />
        <Route path="/collaboration" element={<pages.Collaboration />} />
        <Route path="/mentorship" element={pages.Mentorship ? <pages.Mentorship /> : null} />
        <Route path="/feedback" element={pages.Feedback ? <pages.Feedback /> : null} />
        <Route path="/portfolio" element={<pages.Portfolio />} />
        <Route path="/notifications" element={<pages.Notifications />} />
        <Route path="/profile" element={<pages.Profile />} />
        <Route path="/students/:id" element={<pages.ProfileView />} />
        <Route path="/settings" element={<pages.Settings />} />
        <Route path="/admin" element={isAdmin ? <pages.Admin /> : <Navigate to="/dashboard" />} />
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </Layout>
  )
}
