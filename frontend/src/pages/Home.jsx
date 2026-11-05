import { Link } from 'react-router-dom'
import { useAuth } from '../auth'

export default function Home() {
  const { user } = useAuth()
  return (
    <>
      {/* hero — same composition as the original static design */}
      <section className="mb-14 grid items-center gap-10 md:grid-cols-2">
        <div>
          <p className="eyebrow">STUDENT SKILL EXCHANGE</p>
          <h1 className="h-display text-[clamp(34px,5vw,52px)] leading-tight">
            Build something great.<br />
            <span className="text-brand">Build it together.</span>
          </h1>
          <p className="mt-4 max-w-md text-muted">
            SkillBridge helps students find project teammates, exchange skills and turn ideas into real work.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            {user
              ? <>
                <Link className="btn-primary" to="/dashboard">Go to dashboard</Link>
                <Link className="btn-secondary" to="/projects">Explore projects</Link>
              </>
              : <>
                <Link className="btn-primary" to="/register">Get started</Link>
                <Link className="btn-secondary" to="/projects">Explore projects</Link>
              </>}
          </div>
          <p className="mt-5 text-sm text-muted">A student-first space for learning through collaboration.</p>
        </div>
        <div className="relative grid place-items-center rounded-3xl bg-gradient-to-br from-soft to-white p-10">
          <div className="absolute h-64 w-64 rounded-full border border-brand/15" />
          <div className="absolute h-48 w-48 rounded-full border border-brand/10" />
          <div className="relative w-[min(300px,80%)] rounded-2xl border border-line bg-white p-6 text-center shadow-card">
            <div className="mx-auto mb-3 grid h-12 w-12 place-items-center rounded-xl bg-gradient-to-br from-[#4268ef] to-[#263fae] text-xl text-white">✦</div>
            <strong className="block font-display text-ink">Great work happens together</strong>
            <p className="mt-1 text-xs text-muted">Share skills · Find teammates · Build projects</p>
            <div className="mt-4 flex justify-center -space-x-2">
              {['AK', 'RM', 'PS', '+4'].map((s, i) => (
                <span key={i} className="grid h-8 w-8 place-items-center rounded-full border-2 border-white bg-soft text-[10px] font-bold text-brand">{s}</span>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* feature trio */}
      <section className="mb-14">
        <div className="mb-7">
          <p className="eyebrow">HOW IT WORKS</p>
          <h2 className="h-display text-3xl">Everything you need to collaborate</h2>
        </div>
        <div className="grid gap-5 md:grid-cols-3">
          {[
            ['⌕', 'Discover projects', 'Explore ideas from students and find work that interests you.'],
            ['◎', 'Find your people', 'Connect with teammates based on skills and learning goals.'],
            ['↗', 'Grow together', 'Plan tasks, learn from peers and showcase what you build.'],
          ].map(([icon, title, text]) => (
            <article key={title} className="panel">
              <span className="mb-3 inline-grid h-11 w-11 place-items-center rounded-xl bg-soft text-xl text-brand">{icon}</span>
              <h3 className="font-display font-bold text-ink">{title}</h3>
              <p className="mt-1 text-sm text-muted">{text}</p>
            </article>
          ))}
        </div>
      </section>

      {/* cta strip */}
      {!user && (
        <section className="panel flex flex-wrap items-center justify-between gap-6 bg-gradient-to-r from-[#17243b] to-[#2847bd] text-white">
          <div>
            <h2 className="font-display text-2xl font-extrabold">Your next project starts here.</h2>
            <p className="mt-1 text-white/80">Create a profile and meet students who want to build.</p>
          </div>
          <a className="btn bg-white text-brand hover:-translate-y-px" href="/register">Create free account</a>
        </section>
      )}
    </>
  )
}
