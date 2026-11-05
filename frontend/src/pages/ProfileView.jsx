import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { PageHeading, Panel, StatusBadge, Field, EmptyState, useToast, Spinner } from '../components'
import { get, post } from '../api'

/** Public student profile view (/students/:id) with send-a-message and portfolio preview. */
export default function ProfileView() {
  const { id } = useParams()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [portfolio, setPortfolio] = useState([])
  const [error, setError] = useState('')
  const [dmBody, setDmBody] = useState('')

  useEffect(() => {
    setData(null)
    Promise.allSettled([
      get(`/api/students/${id}`),
      get(`/api/portfolio/user/${id}`),
    ]).then(([p, f]) => {
      if (p.status === 'fulfilled') setData(p.value)
      else setError(p.value?.message || 'Profile not available.')
      if (f.status === 'fulfilled') setPortfolio(f.value)
    })
  }, [id])

  if (error) return <EmptyState icon="🔒" text={error} />
  if (!data) return <Spinner />

  const message = async (e) => {
    e.preventDefault()
    try {
      await post(`/api/messages/with/${id}`, { body: dmBody })
      setDmBody('')
      toast('Message sent ✉️')
    } catch (ex) { toast({ text: ex.message }) }
  }

  return (
    <>
      <PageHeading title={data.fullName} lead={data.email} />

      <div className="grid gap-5 lg:grid-cols-[1.5fr_1fr]">
        <Panel title="About">
          <p className="text-sm text-muted">{data.profile?.bio || 'This student has not written a bio yet.'}</p>
          <div className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
            <p><strong className="text-ink">College:</strong> {data.profile?.college || '—'}</p>
            <p><strong className="text-ink">Course:</strong> {data.profile?.course || '—'}</p>
            <p><strong className="text-ink">Year:</strong> {data.profile?.yearOfStudy || '—'}</p>
            <p><strong className="text-ink">Availability:</strong>
              {' '}{({ ONE_5: '1–5 hrs', SIX_10: '6–10 hrs', ELEVEN_20: '11–20 hrs', TWENTY_PLUS: '20+ hrs' })[data.profile?.hoursPerWeek] || '—'}</p>
            <p className="sm:col-span-2"><strong className="text-ink">Interests:</strong> {data.profile?.interests || '—'}</p>
          </div>
          <div className="mt-4 flex flex-wrap gap-2 text-xs">
            {data.profile?.githubUrl && <a className="tag" href={data.profile.githubUrl} target="_blank" rel="noreferrer">GitHub ↗</a>}
            {data.profile?.linkedinUrl && <a className="tag" href={data.profile.linkedinUrl} target="_blank" rel="noreferrer">LinkedIn ↗</a>}
            {data.profile?.portfolioUrl && <a className="tag" href={data.profile.portfolioUrl} target="_blank" rel="noreferrer">Portfolio ↗</a>}
          </div>
        </Panel>

        <div className="grid gap-5">
          <Panel title="Skills">
            <div className="flex flex-wrap gap-1.5">
              {data.skills.length
                ? data.skills.map(s => <span key={s.skillId} className={`tag ${['ADVANCED', 'EXPERT'].includes(s.level) ? 'tag-teal' : ''}`}>{s.skillName} · {s.level.toLowerCase()}</span>)
                : <EmptyState icon="🎓" text="No skills listed yet." />}
            </div>
          </Panel>
          <Panel title="Message this student">
            <form onSubmit={message}>
              <Field label="Private message">
                <textarea className="field-input" rows={2} value={dmBody} required maxLength={1000}
                          onChange={(e) => setDmBody(e.target.value)} placeholder="Hi! Would you like to…" />
              </Field>
              <button className="btn-primary btn-sm">Send message</button>
            </form>
          </Panel>
        </div>
      </div>

      <Panel title={`Public portfolio (${portfolio.length})`} className="mt-6">
        {portfolio.length === 0 ? <EmptyState icon="🏆" text="No public portfolio entries." />
          : <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {portfolio.map(item => (
              <div key={item.id} className="rounded-xl bg-pagebg p-4">
                <div className="mb-1 flex items-center justify-between">
                  <strong className="text-ink">{item.title}</strong>
                  <span className="tag">{item.tag.toLowerCase()}</span>
                </div>
                <p className="line-clamp-2 text-sm text-muted">{item.description}</p>
              </div>
            ))}
          </div>}
      </Panel>
    </>
  )
}
