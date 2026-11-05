import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Field, useToast } from '../components'
import { AuthShell } from './_AuthShell'
import { api, setToken } from '../api'

export default function ChangePassword() {
  const navigate = useNavigate()
  const toast = useToast()
  const [form, setForm] = useState({ currentPassword: '', newPassword: '' })
  const [error, setError] = useState('')

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await api('/api/auth/password', { method: 'PUT', body: form })
      setToken(null)
      toast('Password updated — log in again with your new password.')
      navigate('/login')
    } catch (ex) {
      setError(ex.message)
    }
  }

  return (
    <AuthShell title="Change password" lead="All your other sessions are logged out after a password change."
      footer={<a href="/settings">Back to settings</a>}>
      <form onSubmit={submit} className="max-w-sm">
        <Field label="Current password">
          <input className="field-input" type="password" required value={form.currentPassword}
                 onChange={(e) => setForm({ ...form, currentPassword: e.target.value })} />
        </Field>
        <Field label="New password" hint="Minimum 8 characters, different from the current one.">
          <input className="field-input" type="password" required minLength={8} value={form.newPassword}
                 onChange={(e) => setForm({ ...form, newPassword: e.target.value })} />
        </Field>
        {error && <p className="mb-3 rounded-lg bg-[#ffeded] px-3 py-2 text-sm text-[#c34d4d]">{error}</p>}
        <button className="btn-primary w-full">Update password</button>
      </form>
    </AuthShell>
  )
}
