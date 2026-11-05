import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'
import { Field, useToast } from '../components'
import { AuthShell } from './_AuthShell'
import { setToken, cacheUser, api } from '../api'

export default function Register() {
  const { register, login } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()
  const [form, setForm] = useState({ fullName: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await register(form.fullName, form.email, form.password)
      const res = await api('/api/auth/login', { method: 'POST', body: { email: form.email, password: form.password } })
      setToken(res.token); cacheUser(res.user)
      window.location.reload() // boot the AuthProvider with the fresh token, then head to profile flow
      toast('Account created 🎉 Set up your profile next.')
      navigate('/profile')
    } catch (ex) {
      setError(ex.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthShell title="Create your account" lead="Join SkillBridge and start collaborating with students everywhere."
      footer={<>Already registered? <Link to="/login">Log in</Link></>}>
      <form onSubmit={submit} className="max-w-sm">
        <Field label="Full name">
          <input className="field-input" required value={form.fullName}
                 onChange={(e) => setForm({ ...form, fullName: e.target.value })} placeholder="Keshitha Kondra" />
        </Field>
        <Field label="College email">
          <input className="field-input" type="email" required value={form.email}
                 onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="you@college.edu" />
        </Field>
        <Field label="Password" hint="At least 8 characters. Stored as a BCrypt hash — never in plain text.">
          <input className="field-input" type="password" required minLength={8} value={form.password}
                 onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="••••••••" />
        </Field>
        {error && <p className="mb-3 rounded-lg bg-[#ffeded] px-3 py-2 text-sm text-[#c34d4d]">{error}</p>}
        <button className="btn-primary w-full" disabled={busy}>{busy ? 'Creating…' : 'Create free account'}</button>
      </form>
    </AuthShell>
  )
}
