import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PageHeading, Panel, StatusBadge, EmptyState, useToast, Spinner } from '../components'
import { get, post } from '../api'

/** U3 / F6 — manage join requests & invitations: accept, reject, or withdraw. */
export default function Requests() {
  const toast = useToast()
  const [rows, setRows] = useState(null)
  const [filter, setFilter] = useState('ALL')

  const load = () => get('/api/requests').then(setRows).catch(() => setRows([]))
  useEffect(() => { load() }, [])

  if (rows === null) return <Spinner />

  const decide = async (id, action) => {
    try {
      await post(`/api/requests/${id}/${action}`, {})
      toast(action === 'accept' ? 'Request accepted ✅' : action === 'reject' ? 'Request declined' : 'Withdrawn')
      load()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const shown = rows.filter(r => filter === 'ALL' || r.audienceName === filter)
  const canDecide = (r) =>
    r.status === 'PENDING' && ((r.audienceName === 'INCOMING' && r.type === 'JOIN_REQUEST') ||
      (r.audienceName === 'OUTGOING' && r.type === 'INVITATION'))

  return (
    <>
      <PageHeading title="Project requests" lead="Join requests and invitations — accept or reject in one click (U3 / F6)."
        action={
          <div className="flex gap-2">
            {['ALL', 'INCOMING', 'OUTGOING'].map(f => (
              <button key={f} className={`${filter === f ? 'btn-primary' : 'btn-secondary'} btn-sm`} onClick={() => setFilter(f)}>
                {f.toLowerCase()}
              </button>
            ))}
          </div>} />

      {shown.length === 0 ? <EmptyState icon="✉️" text="No requests yet. Apply to projects you like or invite suggested students!" />
        : <div className="grid gap-4 md:grid-cols-2">
          {shown.map(r => (
            <Panel key={r.id}>
              <div className="mb-2 flex items-start justify-between gap-3">
                <div>
                  <div className="font-display font-bold text-ink">
                    {r.type === 'INVITATION' ? (r.audienceName === 'OUTGOING' ? '📤 You invited ' : '📩 Invited to ') : (r.audienceName === 'INCOMING' ? '📥 ' : '📤 ')}
                    <Link to={`/projects/${r.projectId}`} className="text-brand no-underline">{r.projectTitle}</Link>
                  </div>
                  <div className="text-xs text-muted">{r.type === 'JOIN_REQUEST' ? `Requested by ${r.requesterName}` : `For ${r.requesterName}`} · {new Date(r.createdAt).toLocaleDateString()}</div>
                </div>
                <StatusBadge value={r.status} />
              </div>
              {r.message && <p className="mb-3 rounded-xl bg-pagebg p-3 text-sm">“{r.message}”</p>}
              <div className="flex flex-wrap gap-2">
                {canDecide(r) && r.type === 'JOIN_REQUEST' && r.audienceName === 'INCOMING' && <>
                  <button className="btn-primary btn-sm" onClick={() => decide(r.id, 'accept')}>Accept</button>
                  <button className="btn-danger btn-sm" onClick={() => decide(r.id, 'reject')}>Reject</button>
                </>}
                {canDecide(r) && r.type === 'INVITATION' && <>
                  <button className="btn-primary btn-sm" onClick={() => decide(r.id, 'accept')}>Accept invitation</button>
                  <button className="btn-danger btn-sm" onClick={() => decide(r.id, 'reject')}>Decline</button>
                </>}
                {r.status === 'PENDING' && r.audienceName === 'OUTGOING' &&
                  <button className="btn-secondary btn-sm" onClick={() => decide(r.id, 'cancel')}>Withdraw</button>}
              </div>
            </Panel>
          ))}
        </div>}
    </>
  )
}
