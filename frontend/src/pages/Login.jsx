import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'
import { Field, useToast } from '../components'
import { AuthShell } from './_AuthShell'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await login(form.email, form.password)
      toast('Welcome back! 👋')
      navigate('/dashboard')
    } catch (ex) {
      setError(ex.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthShell title="Log in" lead="Pick up right where you left off."
      footer={<>New to SkillBridge? <Link to="/register">Create a free account</Link> · <Link to="/forgot-password">Forgot password?</Link></>}>
      <form onSubmit={submit} className="max-w-sm">
        <Field label="Email">
          <input className="field-input" type="email" required value={form.email}
                 onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="you@college.edu" />
        </Field>
        <Field label="Password">
          <input className="field-input" type="password" required value={form.password}
                 onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="••••••••" />
        </Field>
        {error && <p className="mb-3 rounded-lg bg-[#ffeded] px-3 py-2 text-sm text-[#c34d4d]">{error}</p>}
        <button className="btn-primary w-full" disabled={busy}>{busy ? 'Checking…' : 'Log in'}</button>
        <p className="mt-4 text-xs text-muted">Demo accounts all use the password <code>skillbridge1</code> — try as <code>karthik@skillbridge.edu</code> (project owner) or <code>admin@skillbridge.edu</code> (administrator).</p>
      </form>
    </AuthShell>
  )
}
