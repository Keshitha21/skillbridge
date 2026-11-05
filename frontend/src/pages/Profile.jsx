import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PageHeading, Panel, Field, StatusBadge, EmptyState, useToast, Spinner } from '../components'
import { useAuth } from '../auth'
import { get, put, post, del } from '../api'

/** U1 / F2 — my profile: details, availability, visibility and skill tiles. */
export default function Profile() {
  const { user } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [skills, setSkills] = useState([])
  const [form, setForm] = useState(null)

  const load = () => {
    get('/api/profile').then((d) => {
      setData(d)
      setForm({
        college: d.profile?.college || '', course: d.profile?.course || '',
        yearOfStudy: d.profile?.yearOfStudy || '', bio: d.profile?.bio || '',
        interests: d.profile?.interests || '',
        hoursPerWeek: d.profile?.hoursPerWeek || 'SIX_10',
        visibility: d.profile?.visibility || 'PUBLIC',
        githubUrl: d.profile?.githubUrl || '', linkedinUrl: d.profile?.linkedinUrl || '',
        portfolioUrl: d.profile?.portfolioUrl || '',
      })
    }).catch((ex) => toast({ text: ex.message }))
    get('/api/skills').then(setSkills).catch(() => {})
  }
  useEffect(() => { load() }, [])

  if (!data || !form) return <Spinner />

  const save = async (e) => {
    e.preventDefault()
    try {
      await put('/api/profile', {
        ...form,
        yearOfStudy: form.yearOfStudy ? Number(form.yearOfStudy) : null,
      })
      toast('Profile saved ✅'); load()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const addSkill = async (e) => {
    e.preventDefault()
    const d = new FormData(e.target)
    try {
      await post('/api/profile/skills', { skillId: Number(d.get('skillId')), level: d.get('level') })
      toast('Skill added 🎓'); e.target.reset(); load()
    } catch (ex) { toast({ text: ex.message }) }
  }

  const removeSkill = async (skillId) => {
    try { await del(`/api/profile/skills/${skillId}`); load() } catch (ex) { toast({ text: ex.message }) }
  }

  return (
    <>
      <PageHeading title="My profile" lead="Your SkillBridge identity — students use it to find and invite you."
        action={<Link to="/portfolio" className="btn-secondary">My portfolio →</Link>} />

      <div className="grid gap-5 lg:grid-cols-[1.5fr_1fr]">
        <Panel title="Profile details">
          <form onSubmit={save} className="grid gap-4 md:grid-cols-2">
            <Field label="College"><input className="field-input" value={form.college} onChange={(e) => setForm({ ...form, college: e.target.value })} /></Field>
            <Field label="Course"><input className="field-input" value={form.course} onChange={(e) => setForm({ ...form, course: e.target.value })} /></Field>
            <Field label="Year of study">
              <input className="field-input" type="number" min="1" max="6" value={form.yearOfStudy}
                     onChange={(e) => setForm({ ...form, yearOfStudy: e.target.value })} />
            </Field>
            <Field label="Hours available per week">
              <select className="field-input" value={form.hoursPerWeek} onChange={(e) => setForm({ ...form, hoursPerWeek: e.target.value })}>
                <option value="ONE_5">1–5 hours</option><option value="SIX_10">6–10 hours</option>
                <option value="ELEVEN_20">11–20 hours</option><option value="TWENTY_PLUS">20+ hours</option>
              </select>
            </Field>
            <div className="md:col-span-2">
              <Field label="Interests" hint="Comma-separated, e.g. Web development, ML, Hackathons">
                <input className="field-input" value={form.interests} onChange={(e) => setForm({ ...form, interests: e.target.value })} />
              </Field>
              <Field label="Bio">
                <textarea className="field-input" rows={3} value={form.bio} onChange={(e) => setForm({ ...form, bio: e.target.value })} />
              </Field>
            </div>
            <Field label="GitHub"><input className="field-input" value={form.githubUrl} onChange={(e) => setForm({ ...form, githubUrl: e.target.value })} /></Field>
            <Field label="LinkedIn"><input className="field-input" value={form.linkedinUrl} onChange={(e) => setForm({ ...form, linkedinUrl: e.target.value })} /></Field>
            <Field label="Portfolio site"><input className="field-input" value={form.portfolioUrl} onChange={(e) => setForm({ ...form, portfolioUrl: e.target.value })} /></Field>
            <Field label="Profile visibility" hint="Private profiles are hidden from search">
              <select className="field-input" value={form.visibility} onChange={(e) => setForm({ ...form, visibility: e.target.value })}>
                <option value="PUBLIC">Public</option><option value="PRIVATE">Private</option>
              </select>
            </Field>
            <div className="md:col-span-2 flex gap-2">
              <button className="btn-primary">Save profile</button>
              <Link to="/settings" className="btn-secondary">Account settings</Link>
            </div>
          </form>
        </Panel>

        <div className="grid gap-5">
          <Panel title="My skills">
            <div className="mb-4 flex flex-wrap gap-1.5">
              {data.skills.length === 0 && <EmptyState icon="🎓" text="Add skills so teams can find you." />}
              {data.skills.map(s => (
                <span key={s.skillId} className="tag">
                  {s.skillName} · {s.level.toLowerCase()}
                  <button className="ml-1 text-[#c34d4d]" onClick={() => removeSkill(s.skillId)} title="Remove">✕</button>
                </span>
              ))}
            </div>
            <form onSubmit={addSkill} className="flex flex-col gap-2 sm:flex-row">
              <select className="field-input" name="skillId" required>
                <option value="">Choose a skill…</option>
                {skills.filter(s => !data.skills.some(x => x.skillId === s.id))
                  .map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
              <select className="field-input" name="level" defaultValue="BEGINNER">
                <option>BEGINNER</option><option>INTERMEDIATE</option><option>ADVANCED</option><option>EXPERT</option>
              </select>
              <button className="btn-ghost">Add</button>
            </form>
          </Panel>
          <Panel title="Account">
            <p className="text-sm"><strong>{data.fullName}</strong><br /><span className="text-muted">{data.email}</span></p>
            <div className="mt-3 flex gap-2">
              <StatusBadge value={user?.role || 'STUDENT'} />
              <Link className="btn-secondary btn-sm" to="/settings">Change password →</Link>
            </div>
          </Panel>
        </div>
      </div>
    </>
  )
}
