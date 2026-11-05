import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'
import { Field, useToast } from '../components'
import { api, setToken, cacheUser } from '../api'

export function AuthShell({ title, lead, children, footer }) {
  return (
    <div className="grid gap-8 md:grid-cols-[1fr_1fr]">
      <div className="panel">
        <p className="eyebrow">STUDENT SKILL EXCHANGE</p>
        <h1 className="h-display mb-1 text-3xl">{title}</h1>
        <p className="mb-6 text-sm text-muted">{lead}</p>
        {children}
      </div>
      <div className="panel bg-gradient-to-br from-soft to-white">
        <div className="grid h-full place-items-center text-center">
          <div>
            <div className="mb-4 grid h-16 w-16 place-items-center rounded-2xl bg-gradient-to-br from-[#4268ef] to-[#263fae] text-3xl text-white shadow-card">✦</div>
            <strong className="block font-display text-lg text-ink">Great work happens together</strong>
            <p className="mt-1 text-sm text-muted">Share skills · Find teammates · Build projects</p>
          </div>
        </div>
      </div>
      <div className="md:col-span-2 text-center text-sm text-muted">{footer}</div>
    </div>
  )
}
