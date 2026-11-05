import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { PageHeading, Panel, EmptyState, Spinner, useToast } from '../components'
import { get, post } from '../api'

/** U6 / F10 — private student-to-student messages (project group chat lives on the project page). */
export default function Collaboration() {
  const toast = useToast()
  const [params, setParams] = useSearchParams()
  const [threads, setThreads] = useState(null)
  const [students, setStudents] = useState([])
  const activePartner = Number(params.get('dm')) || null
  const [messages, setMessages] = useState([])
  const [draft, setDraft] = useState('')

  useEffect(() => {
    get('/api/messages/threads').then(setThreads).catch(() => setThreads([]))
    get('/api/projects?mine=true').catch(() => {})
  }, [])

  useEffect(() => {
    // directory for starting new conversations (public profiles)
    get('/api/students').then((s) => setStudents(s || [])).catch(() => setStudents([]))
  }, [])

  useEffect(() => {
    if (!activePartner) { setMessages([]); return }
    get(`/api/messages/with/${activePartner}`).then(setMessages).catch(() => setMessages([]))
  }, [activePartner])

  const send = async (e) => {
    e.preventDefault()
    try {
      await post(`/api/messages/with/${activePartner}`, { body: draft })
      setDraft('')
      setMessages(await get(`/api/messages/with/${activePartner}`))
      get('/api/messages/threads').then(setThreads).catch(() => {})
    } catch (ex) { toast({ text: ex.message }) }
  }

  const partnerName = (id) =>
    threads?.find(t => t.partnerId === id)?.partnerName ||
    students.find(s => s.userId === id)?.fullName || `Student #${id}`

  return (
    <>
      <PageHeading title="Collaboration" lead="Private conversations with other students." />

      <div className="grid gap-5 lg:grid-cols-[300px_1fr]">
        {/* thread list */}
        <Panel title="Inbox" className="max-h-[70vh] overflow-y-auto">
          {threads === null ? <Spinner /> : threads.length === 0
            ? <EmptyState icon="💌" text="No conversations yet — start one from the student list below." />
            : <ul className="divide-y divide-line">
              {threads.map(t => (
                <li key={t.partnerId}>
                  <button onClick={() => setParams({ dm: String(t.partnerId) })}
                          className={`w-full rounded-lg px-2 py-2.5 text-left ${activePartner === t.partnerId ? 'bg-soft' : 'hover:bg-pagebg'}`}>
                    <div className="flex items-center justify-between">
                      <strong className="text-sm text-ink">{t.partnerName}</strong>
                      {t.unread > 0 && <span className="badge badge-red">{t.unread} new</span>}
                    </div>
                    <div className="line-clamp-1 text-xs text-muted">{t.lastMessage}</div>
                  </button>
                </li>
              ))}
            </ul>}

          <div className="mt-4 border-t border-line pt-4">
            <h4 className="field-label mb-2">Start a new conversation</h4>
            <select className="field-input" value="" onChange={(e) => e.target.value && setParams({ dm: e.target.value })}>
              <option value="">Pick a student…</option>
              {students.filter(s => s.userId !== undefined).map(s => (
                <option key={s.userId} value={s.userId}>{s.fullName}</option>
              ))}
            </select>
          </div>
        </Panel>

        {/* conversation */}
        <Panel title={activePartner ? `Chat with ${partnerName(activePartner)}` : 'Pick a conversation'} className="min-h-[60vh]">
          {!activePartner ? <EmptyState icon="💬" text="Select a thread or start a new one." /> : <>
            <div className="mb-4 max-h-[46vh] space-y-3 overflow-y-auto rounded-xl bg-pagebg p-4">
              {messages.map(m => (
                <div key={m.id} className={`max-w-[80%] rounded-2xl px-4 py-2 text-sm ${m.senderId === activePartner ? 'bg-white border border-line' : 'ml-auto bg-brand text-white'}`}>
                  <p className="whitespace-pre-line">{m.body}</p>
                  <div className={`mt-1 text-[10px] ${m.senderId === activePartner ? 'text-muted' : 'text-white/70'}`}>
                    {new Date(m.createdAt).toLocaleString()}
                  </div>
                </div>
              ))}
              {messages.length === 0 && <EmptyState icon="👋" text="No messages yet — say hello!" />}
            </div>
            <form className="flex gap-2" onSubmit={send}>
              <input className="field-input" placeholder={`Message ${partnerName(activePartner)}…`} value={draft}
                     onChange={(e) => setDraft(e.target.value)} required maxLength={1000} />
              <button className="btn-primary">Send</button>
            </form>
          </>}
        </Panel>
      </div>
    </>
  )
}
