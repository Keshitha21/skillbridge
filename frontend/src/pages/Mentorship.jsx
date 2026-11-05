import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PageHeading, Panel, StatusBadge, Field, EmptyState, useToast, Spinner } from '../components'
import { api, get, post, del } from '../api'

const TASK_NEXT = {
  TODO: ['IN_PROGRESS', 'DONE'],
  IN_PROGRESS: ['DONE', 'TODO'],
  DONE: ['TODO'], // redo a finished step
}

/** U8 / F13 — mentorship & skill exchange: listings with clear TEACH/LEARN labels,
 *  requests inbox, learning workspaces (teaching files + learning-plan tasks),
 *  completion and feedback. Every action sends the partner a notification. */
export default function Mentorship() {
  const toast = useToast()
  const [offers, setOffers] = useState(null)
  const [requests, setRequests] = useState([])
  const [skills, setSkills] = useState([])
  const [filters, setFilters] = useState({ type: '', status: '', q: '' })
  const [showForm, setShowForm] = useState(false)
  const [openOffer, setOpenOffer] = useState(null) // offer under workspace view
  const me = JSON.parse(localStorage.getItem('skillbridge_user') || 'null')

  const load = () => {
    const qs = new URLSearchParams()
    if (filters.type) qs.set('type', filters.type)
    if (filters.status) qs.set('status', filters.status)
    if (filters.q) qs.set('q', filters.q)
    Promise.allSettled([
      get(`/api/mentorship?${qs}`),
      get('/api/mentorship/requests'),
      get('/api/skills'),
    ]).then(([o, r, s]) => {
      if (o.status === 'fulfilled') setOffers(o.value)
      if (r.status === 'fulfilled') setRequests(r.value)
      if (s.status === 'fulfilled') setSkills(s.value)
    })
  }
  useEffect(() => { load() }, [filters])

  // ---------- create listing ----------
  const createOffer = async (e) => {
    e.preventDefault()
    const data = new FormData(e.target)
    const body = {
      type: data.get('type'), title: data.get('title'), description: data.get('description'),
      skillId: data.get('skillId') ? Number(data.get('skillId')) : null,
      maxParticipants: Number(data.get('maxParticipants') || 1),
    }
    try {
      await post('/api/mentorship', body)
      toast('Listing created 🎯')
      setShowForm(false); load()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const requestJoin = async (offer) => {
    const isMentorOffer = offer.type === 'OFFER'
    const message = prompt(`Message to the host of "${offer.title}":`,
      isMentorOffer ? 'Hi! I would like to learn this from you. When can we start?' : 'Hi! I can mentor you on this. Shall we plan a session?')
    if (message === null) return
    try {
      await post(`/api/mentorship/${offer.id}/requests`, { message })
      toast(isMentorOffer ? 'Request sent — you asked to join as a learner ✉️' : 'Volunteered to teach ✉️')
      load()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const decide = async (requestId, action) => {
    try {
      await post(`/api/mentorship/requests/${requestId}/${action}`, {})
      toast(action === 'accept' ? 'Accepted 🤝 — a workspace is now open' : 'Declined')
      load()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const complete = async (requestId) => {
    try {
      await post(`/api/mentorship/requests/${requestId}/complete`, {})
      toast('Marked complete — feedback is now open ⭐')
      load()
    } catch (ex) { toast({ text: ex.message }) }
  }

  if (offers === null) return <Spinner />

  // group my requests: incoming (on my listings) vs outgoing
  const incoming = requests.filter(r => r.offerOwnerId === (me && me.id))
  const outgoing = requests.filter(r => r.offerOwnerId !== (me && me.id))
  const accepted = requests.filter(r => r.status === 'ACCEPTED')

  return (
    <>
      <PageHeading title="Mentorship & skill exchange"
        lead="Offer what you know, request what you want to learn — then work in a shared workspace (U8 / F13)."
        action={<button className="btn-primary" onClick={() => setShowForm(!showForm)}>{showForm ? 'Close form' : '+ New listing'}</button>} />

      {showForm && (
        <Panel title="Create a mentorship listing" className="mb-6">
          <form onSubmit={createOffer} className="grid gap-4 md:grid-cols-2">
            <Field label="I want to…">
              <select className="field-input" name="type">
                <option value="OFFER">Teach / mentor others 🧑‍🏫</option>
                <option value="REQUEST">Learn from someone 🙋</option>
              </select>
            </Field>
            <Field label="Related skill">
              <select className="field-input" name="skillId">
                <option value="">— any skill —</option>
                {skills.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </Field>
            <Field label="Title">
              <input className="field-input" name="title" required maxLength={120} placeholder="Spring Boot basics 1:1" />
            </Field>
            <Field label="Max participants">
              <input className="field-input" type="number" name="maxParticipants" min="1" max="20" defaultValue="2" />
            </Field>
            <div className="md:col-span-2">
              <Field label="Description">
                <textarea className="field-input" name="description" rows={3} placeholder="What will the sessions cover? When?" />
              </Field>
            </div>
            <div className="md:col-span-2">
              <button className="btn-primary">Publish listing</button>
            </div>
          </form>
        </Panel>
      )}

      {/* ---------- workspaces for accepted activities ---------- */}
      {accepted.length > 0 && (
        <section className="mb-8">
          <h2 className="mb-3 text-xl font-bold text-ink">My learning workspaces</h2>
          <div className="grid gap-4">
            {accepted.map(r => (
              <Workspace key={r.id} request={r} me={me} isOwner={r.offerOwnerId === (me && me.id)} refresh={load} />
            ))}
          </div>
        </section>
      )}

      {/* ---------- request inbox + my outgoing requests ---------- */}
      <div className="mb-8 grid gap-5 lg:grid-cols-2">
        <Panel title="Requests on my listings">
          {incoming.length === 0 ? <EmptyState icon="📨" text="Nothing requested yet. People who want your listing will appear here." />
            : <ul className="divide-y divide-line text-sm">
              {incoming.map(r => (
                <li key={r.id} className="py-2.5">
                  <div className="flex items-center justify-between gap-2">
                    <span>
                      <strong>{r.requesterName}</strong>
                      {r.offerType === 'OFFER'
                        ? <span className="text-muted"> wants to <b>learn from you</b> in “{r.offerTitle}”</span>
                        : <span className="text-muted"> wants to <b>teach you</b> in “{r.offerTitle}”</span>}
                    </span>
                    <StatusBadge value={r.status} />
                  </div>
                  {r.message && <p className="mt-1 rounded-lg bg-pagebg px-2 py-1 text-xs">“{r.message}”</p>}
                  <div className="mt-1.5 flex flex-wrap gap-1.5">
                    {r.status === 'PENDING' && <>
                      <button className="btn-primary btn-sm" onClick={() => decide(r.id, 'accept')}>
                        {r.offerType === 'OFFER' ? 'Accept learner' : 'Accept as my mentor'}
                      </button>
                      <button className="btn-danger btn-sm" onClick={() => decide(r.id, 'decline')}>Decline</button>
                    </>}
                    {r.status === 'ACCEPTED' && <>
                      <button className="btn-secondary btn-sm" onClick={() => setOpenOffer(r.offerId)}>Open workspace</button>
                      <button className="btn-secondary btn-sm" onClick={() => complete(r.id)}>Mark completed</button>
                    </>}
                  </div>
                </li>
              ))}
            </ul>}
        </Panel>
        <Panel title="My requests">
          {outgoing.length === 0 ? <EmptyState icon="🗂️" text="Requests you send will be tracked here." />
            : <ul className="divide-y divide-line text-sm">
              {outgoing.map(r => (
                <li key={r.id} className="py-2.5">
                  <div className="flex items-center justify-between">
                    <span>
                      {r.offerType === 'OFFER'
                        ? <>learning “{r.offerTitle}” — mentor: <strong>{r.offerOwnerName}</strong></>
                        : <>volunteered to teach “{r.offerTitle}” (host: {r.offerOwnerName})</>}
                    </span>
                    <StatusBadge value={r.status} />
                  </div>
                  <div className="mt-1.5 flex gap-1.5">
                    {r.status === 'ACCEPTED' && <>
                      <button className="btn-secondary btn-sm" onClick={() => setOpenOffer(r.offerId)}>Open workspace</button>
                      <button className="btn-secondary btn-sm" onClick={() => complete(r.id)}>Mark completed</button>
                    </>}
                    {r.status === 'COMPLETED' &&
                      <Link to={`/feedback?offer=${r.offerId}`} className="btn-ghost btn-sm">⭐ Leave feedback</Link>}
                  </div>
                </li>
              ))}
            </ul>}
        </Panel>
      </div>

      {/* ---------- listing catalogue with TEACH / LEARN labels ---------- */}
      <div className="mb-5 flex flex-wrap gap-2">
        {[['', 'All listings'], ['OFFER', 'Teaching available'], ['REQUEST', 'Wants to learn']].map(([v, label]) => (
          <button key={v} className={`${filters.type === v ? 'btn-primary' : 'btn-secondary'} btn-sm`}
                  onClick={() => setFilters({ ...filters, type: v })}>{label}</button>
        ))}
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {offers.map(o => {
          const mine = me && o.ownerId === me.id
          return (
            <Panel key={o.id}>
              <div className="mb-2 flex items-start justify-between gap-2">
                <h3 className="font-bold text-ink">{o.title}</h3>
                <span className={`badge ${o.type === 'OFFER' ? 'badge-green' : 'badge-amber'}`}>
                  {o.type === 'OFFER' ? '🧑‍🏫 Will teach' : '🙋 Wants to learn'}
                </span>
              </div>
              <p className="mb-3 text-sm text-muted">{o.description}</p>
              <div className="flex flex-wrap gap-1.5">
                {o.skillName && <span className="tag">{o.skillName}</span>}
                <StatusBadge value={o.status} />
              </div>
              <div className="mt-3 text-xs text-muted">
                {o.type === 'OFFER'
                  ? <>👤 Mentor: <b>{o.ownerName}</b> · {o.activeParticipants}/{o.maxParticipants} learner spots filled</>
                  : <>👤 Learner: <b>{o.ownerName}</b> · mentoring spots {o.activeParticipants}/{o.maxParticipants} filled</>}
              </div>
              {mine
                ? <button className="btn-secondary btn-sm mt-3" onClick={() => setOpenOffer(o.id)}>My listing — open workspace</button>
                : o.type === 'OFFER'
                  ? <button className="btn-primary btn-sm mt-3" onClick={() => requestJoin(o)}>Take mentorship (join to learn)</button>
                  : <button className="btn-primary btn-sm mt-3" onClick={() => requestJoin(o)}>Teach them (volunteer as mentor)</button>}
            </Panel>
          )
        })}
        {offers.length === 0 && <EmptyState text="No listings match these filters." />}
      </div>

      {openOffer && (
        <Workspace offerId={openOffer} me={me} refresh={load} onClose={() => setOpenOffer(null)} />
      )}
    </>
  )
}

/** Learning workspace: teaching files (upload/download/delete) + learning-plan tasks.
 *  Shown inside a modal-style panel; used by owners and accepted participants. */
function Workspace({ request, offerId: offerIdProp, me, isOwner, refresh, onClose }) {
  const toast = useToast()
  const offerId = request ? request.offerId : offerIdProp
  const [offer, setOffer] = useState(null)
  const [tasks, setTasks] = useState(null)
  const [files, setFiles] = useState(null)
  const [role, setRole] = useState(isOwner) // owner sees exact permissions; participants resolved via API

  useEffect(() => {
    Promise.allSettled([
      get(`/api/mentorship/${offerId}`),
      get(`/api/mentorship/${offerId}/tasks`),
      get(`/api/mentorship/${offerId}/files`),
    ]).then(([o, t, f]) => {
      if (o.status === 'fulfilled') {
        setOffer(o.value)
        setRole(o.value.ownerId === (me && me.id))
      }
      if (t.status === 'fulfilled') setTasks(t.value)
      if (f.status === 'fulfilled') setFiles(f.value)
    })
  }, [offerId])

  const loaded = tasks !== null && files !== null

  const upload = async (e) => {
    e.preventDefault()
    const input = e.target.elements.file
    if (!input.files || !input.files[0]) { toast({ text: 'Choose a file first.' }); return }
    const body = new FormData()
    body.append('file', input.files[0])
    try {
      const res = await api(`/api/mentorship/${offerId}/files`, { method: 'POST', body })
      toast(`Uploaded "${res.fileName}" 📎`)
      e.target.reset()
      setFiles(await get(`/api/mentorship/${offerId}/files`))
      refresh()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const download = async (f) => {
    try {
      const res = await api(`/api/mentorship/files/${f.id}/download`, { raw: true })
      if (!res.ok) throw new Error(`Download failed (${res.status})`)
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = f.fileName || 'file'
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
    } catch (ex) { toast({ text: ex.message }) }
  }

  const removeFile = async (f) => {
    try {
      await del(`/api/mentorship/files/${f.id}`)
      toast('File removed')
      setFiles(await get(`/api/mentorship/${offerId}/files`))
      refresh()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const addTask = async (e) => {
    e.preventDefault()
    const data = new FormData(e.target)
    const assigneeId = data.get('assigneeId')
    try {
      await post(`/api/mentorship/${offerId}/tasks`, {
        title: data.get('title'),
        assigneeId: assigneeId ? Number(assigneeId) : null,
      })
      toast('Learning task added 📋')
      e.target.reset()
      setTasks(await get(`/api/mentorship/${offerId}/tasks`))
    } catch (ex) { toast({ text: ex.message }) }
  }

  const moveTask = async (t, status) => {
    try {
      await post(`/api/mentorship/tasks/${t.id}/status`, { status })
      toast(`Task moved to ${status.replace(/_/g, ' ').toLowerCase()}`)
      setTasks(await get(`/api/mentorship/${offerId}/tasks`))
      refresh()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const canMove = (t) =>
    t.createdById === (me && me.id)
    || t.assigneeId === (me && me.id)
    || role // owner can always steer the plan

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4" onClick={onClose}>
      <div className="max-h-[88vh] w-full max-w-3xl overflow-y-auto" onClick={e => e.stopPropagation()}>
        <Panel className="mb-0">
          <div className="mb-4 flex items-start justify-between gap-3">
            <div>
              <h2 className="text-xl font-bold text-ink">Workspace — {offer ? offer.title : (request && request.offerTitle)}</h2>
              <p className="mt-0.5 text-xs text-muted">
                {offer
                  ? <>{offer.type === 'OFFER' ? <>Mentor: <b>{offer.ownerName}</b> · you are learning with them</> : <>Learner: <b>{offer.ownerName}</b> · you are mentoring them</>}</>
                  : 'Loading…'}
              </p>
            </div>
            <button className="btn-secondary btn-sm" onClick={onClose}>Close ✕</button>
          </div>

          {!loaded ? <Spinner /> : (
            <div className="grid gap-5 lg:grid-cols-2">
              {/* ---------- learning-plan tasks ---------- */}
              <div>
                <h3 className="mb-2 font-bold text-ink">📋 Learning plan (tasks)</h3>
                <form onSubmit={addTask} className="mb-3 flex gap-2">
                  <input className="field-input" name="title" required maxLength={150} placeholder="Next step, e.g. 'Build a small REST demo'" />
                  <select className="field-input w-32" name="assigneeId">
                    <option value="">assign to…</option>
                    {offer && <option value={offer.ownerId}>{offer.ownerName}</option>}
                    {me && <option value={me.id}>me ({me.fullName})</option>}
                  </select>
                  <button className="btn-primary btn-sm">Add</button>
                </form>
                {tasks.length === 0 && <p className="text-sm text-muted">No learning steps yet — add the first one above.</p>}
                <ul className="space-y-2">
                  {tasks.map(t => (
                    <li key={t.id} className="panel p-3">
                      <div className="flex items-start justify-between gap-2">
                        <strong className="text-sm text-ink">{t.title}</strong>
                        <StatusBadge value={t.status} />
                      </div>
                      <div className="mt-1 text-xs text-muted">
                        {t.assigneeName ? `👤 ${t.assigneeName}` : '👥 shared'} · added by {t.createdByName}
                      </div>
                      <div className="mt-2 flex flex-wrap gap-1.5">
                        {canMove(t)
                          ? TASK_NEXT[t.status]?.map(s => (
                            <button key={s} className="btn-ghost btn-sm" onClick={() => moveTask(t, s)}>
                              {s === 'DONE' ? '✅ done' : s.replace(/_/g, ' ').toLowerCase()}
                            </button>
                          ))
                          : <span className="text-[11px] text-muted">🔒 only {t.assigneeName || 'the owner'} can move this</span>}
                      </div>
                    </li>
                  ))}
                </ul>
              </div>

              {/* ---------- teaching files ---------- */}
              <div>
                <h3 className="mb-2 font-bold text-ink">📎 Teaching files</h3>
                <form onSubmit={upload} className="mb-3">
                  <input type="file" name="file" required
                    accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.csv,.png,.jpg,.jpeg,.gif,.webp,.zip"
                    aria-label="Choose file" className="field-input py-1.5 text-xs" />
                  <button className="btn-primary btn-sm mt-2">Upload teaching file</button>
                  <p className="mt-1 text-[11px] text-muted">Notes, slides, exercises, code — max 10 MB. Your partner is notified instantly.</p>
                </form>
                {files.length === 0 && <p className="text-sm text-muted">No files shared in this activity yet.</p>}
                <ul className="space-y-2">
                  {files.map(f => (
                    <li key={f.id} className="panel flex items-center justify-between gap-2 p-3">
                      <div>
                        <strong className="block text-sm text-ink">{f.fileName}</strong>
                        <span className="text-xs text-muted">by {f.uploaderName} · {f.sizeBytes} B</span>
                      </div>
                      <div className="flex gap-2">
                        <button className="btn-ghost btn-sm" onClick={() => download(f)}>Download</button>
                        {(f.uploaderId === (me && me.id) || (offer && offer.ownerId === (me && me.id))) &&
                          <button className="btn-danger btn-sm" onClick={() => removeFile(f)}>✕</button>}
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          )}

          <div className="mt-5 flex flex-wrap gap-2 border-t border-line pt-4">
            <button className="btn-secondary btn-sm"
              onClick={async () => {
                if (!request) { toast({ text: 'Open a workspace from a request row to mark it complete.' }); return }
                try {
                  await post(`/api/mentorship/requests/${request.id}/complete`, {})
                  toast('Activity completed ⭐ — feedback unlocked for both sides')
                  onClose(); refresh()
                } catch (ex) { toast({ text: ex.message }) }
              }}>
              🏁 Mark activity completed
            </button>
            <Link className="btn-ghost btn-sm" to={`/feedback?offer=${offerId}`}>⭐ Feedback page</Link>
          </div>
        </Panel>
      </div>
    </div>
  )
}
