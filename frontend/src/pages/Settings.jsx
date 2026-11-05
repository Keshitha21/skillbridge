import { useState } from 'react'
import { Link } from 'react-router-dom'
import { PageHeading, Panel, Field, useToast } from '../components'
import { api, setToken } from '../api'

/** Account security: change password (F1) + links to visibility controls. */
export default function Settings() {
  const toast = useToast()
  const [form, setForm] = useState({ currentPassword: '', newPassword: '' })
  const [error, setError] = useState('')

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await api('/api/auth/password', { method: 'PUT', body: form })
      setToken(null)
      toast('Password updated — please log in again.')
      window.location.href = '/login'
    } catch (ex) { setError(ex.message) }
  }

  return (
    <>
      <PageHeading title="Settings" lead="Account security and preferences." />
      <div className="grid gap-5 lg:grid-cols-2">
        <Panel title="Change password">
          <form onSubmit={submit} className="max-w-sm">
            <Field label="Current password">
              <input className="field-input" type="password" required value={form.currentPassword}
                     onChange={(e) => setForm({ ...form, currentPassword: e.target.value })} />
            </Field>
            <Field label="New password" hint="Min 8 characters, must differ from the current one.">
              <input className="field-input" type="password" required minLength={8} value={form.newPassword}
                     onChange={(e) => setForm({ ...form, newPassword: e.target.value })} />
            </Field>
            {error && <p className="mb-3 rounded-lg bg-[#ffeded] px-3 py-2 text-sm text-[#c34d4d]">{error}</p>}
            <button className="btn-primary">Update password</button>
          </form>
          <p className="mt-4 text-xs text-muted">Changing your password logs out all other sessions for security.</p>
        </Panel>
        <Panel title="Privacy & data">
          <ul className="space-y-2 text-sm text-muted">
            <li>🔐 Passwords are stored as BCrypt hashes — never recoverable in plain text.</li>
            <li>⏱ Login sessions use opaque tokens valid for 30 days.</li>
            <li>👁 Make your profile private from <Link to="/profile">your profile page</Link>.</li>
            <li>🧹 Forgot your password? Use <Link to="/forgot-password">account recovery</Link>.</li>
          </ul>
        </Panel>
      </div>
    </>
  )
}
