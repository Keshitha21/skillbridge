import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Field, useToast } from '../components'
import { AuthShell } from './_AuthShell'
import { post } from '../api'

export default function ForgotPassword() {
  const navigate = useNavigate()
  const toast = useToast()
  const [email, setEmail] = useState('')
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState(null)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      const res = await post('/api/auth/forgot-password', { email })
      setResult(res.message)
    } catch (ex) {
      setResult(ex.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthShell title="Recover your account" lead="We create a one-hour reset token and show it here (local mode — no mail server needed)."
      footer={<a href="/login">Back to log in</a>}>
      <form onSubmit={submit} className="max-w-sm">
        <Field label="Account email">
          <input className="field-input" type="email" required value={email}
                 onChange={(e) => setEmail(e.target.value)} placeholder="you@college.edu" />
        </Field>
        <button className="btn-primary w-full" disabled={busy}>{busy ? 'Working…' : 'Request reset token'}</button>
      </form>
      {result && (
        <div className="mt-5 rounded-xl border border-line bg-soft p-4 text-sm">
          <p className="mb-2 text-muted">{result.split('Local reset token:')[0]}</p>
          {result.includes('Local reset token:') && (
            <>
              <code className="block break-all rounded-lg bg-white px-3 py-2 text-xs">{result.split('Local reset token:')[1]}</code>
              <a href={`/reset-password?token=${result.split('Local reset token:')[1].trim()}`} className="btn-primary mt-4">Continue to reset →</a>
            </>
          )}
        </div>
      )}
    </AuthShell>
  )
}
