import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Field, useToast } from '../components'
import { AuthShell } from './_AuthShell'
import { post } from '../api'

export default function ResetPassword() {
  const navigate = useNavigate()
  const toast = useToast()
  const [params] = useSearchParams()
  const [form, setForm] = useState({ resetToken: params.get('token') || '', newPassword: '' })
  const [error, setError] = useState('')

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await post('/api/auth/reset-password', form)
      toast('Password reset 🔐')
      navigate('/login')
    } catch (ex) {
      setError(ex.message)
    }
  }

  return (
    <AuthShell title="Set a new password" lead="Paste the reset token you received and choose a new password."
      footer={<a href="/forgot-password">Request a new token</a>}>
      <form onSubmit={submit} className="max-w-sm">
        <Field label="Reset token">
          <input className="field-input" required value={form.resetToken}
                 onChange={(e) => setForm({ ...form, resetToken: e.target.value })} />
        </Field>
        <Field label="New password" hint="Minimum 8 characters.">
          <input className="field-input" type="password" required minLength={8} value={form.newPassword}
                 onChange={(e) => setForm({ ...form, newPassword: e.target.value })} />
        </Field>
        {error && <p className="mb-3 rounded-lg bg-[#ffeded] px-3 py-2 text-sm text-[#c34d4d]">{error}</p>}
        <button className="btn-primary w-full">Reset password</button>
      </form>
    </AuthShell>
  )
}
