import { useEffect, useMemo, useState } from 'react'
import {
 Activity,
 ArrowDownLeft,
 ArrowLeftRight,
 ArrowUpRight,
 BarChart3,
 Bell,
 Check,
 ChevronDown,
 CircleHelp,
 CreditCard,
 FileText,
 Gauge,
 KeyRound,
 Layers3,
 LoaderCircle,
 LockKeyhole,
 LogOut,
 Menu,
 Plus,
 RefreshCw,
 Search,
 ShieldCheck,
 Sparkles,
 WalletCards,
 X,
 Zap,
} from 'lucide-react'
import {
 Area,
 AreaChart,
 Bar,
 BarChart,
 CartesianGrid,
 Legend,
 ResponsiveContainer,
 Tooltip,
 XAxis,
 YAxis,
} from 'recharts'
import {
 api,
 clearSession,
 errorMessage,
 login,
 routes,
 unwrap,
} from './api'

type Row = Record<string, any>

type Page =
    | 'Overview'
    | 'Transactions'
    | 'Gateways'
    | 'Credentials'
    | 'Payment attempts'
    | 'Smart routing'
    | 'Reports'

const navigation: { label: Page; icon: any; group: string }[] = [
 { label: 'Overview', icon: Gauge, group: 'WORKSPACE' },
 { label: 'Transactions', icon: ArrowLeftRight, group: 'WORKSPACE' },
 { label: 'Gateways', icon: Layers3, group: 'PAYMENTS' },
 { label: 'Credentials', icon: KeyRound, group: 'PAYMENTS' },
 { label: 'Payment attempts', icon: RefreshCw, group: 'PAYMENTS' },
 { label: 'Smart routing', icon: Zap, group: 'OPTIMIZATION' },
 { label: 'Reports', icon: BarChart3, group: 'OPTIMIZATION' },
]

const fallbackProviders = [
 {
  id: 1,
  name: 'Razorpay',
  code: 'RAZORPAY',
  status: 'Enabled',
  methods: 'UPI, Cards, Net banking',
 },
 {
  id: 2,
  name: 'PayU',
  code: 'PAYU',
  status: 'Enabled',
  methods: 'UPI, Cards, Net banking',
 },
]

function cash(value: any, currency = 'INR') {
 try {
  return new Intl.NumberFormat('en-IN', {
   style: 'currency',
   currency,
   maximumFractionDigits: 2,
  }).format(Number(value || 0))
 } catch {
  return `${currency} ${value || 0}`
 }
}

function compact(value: any) {
 return new Intl.NumberFormat('en-IN', {
  notation: 'compact',
  maximumFractionDigits: 1,
 }).format(Number(value || 0))
}

function pct(value: any) {
 return Math.round(Number(value || 0) * 10) / 10
}

function volumeByCurrency(map: any) {
 const entries = Object.entries(map || {})
 if (!entries.length) return '—'
 return entries
     .map(([code, amount]) => cash(amount, code))
     .join(' · ')
}

function MiniTable({
                    columns,
                    rows,
                    empty,
                   }: {
 columns: { label: string; render: (row: Row) => any }[]
 rows: Row[]
 empty: string
}) {
 if (!rows.length) return <p className="footnote">{empty}</p>

 return (
     <div className="table-wrap">
      <table style={{ minWidth: 0 }}>
       <thead>
       <tr>
        {columns.map((column) => (
            <th key={column.label}>{column.label}</th>
        ))}
       </tr>
       </thead>
       <tbody>
       {rows.map((row, index) => (
           <tr key={index}>
            {columns.map((column) => (
                <td key={column.label}>{column.render(row)}</td>
            ))}
           </tr>
       ))}
       </tbody>
      </table>
     </div>
 )
}

function date(value: any) {
 if (!value) return '—'

 const parsedDate = new Date(value)

 return Number.isNaN(parsedDate.getTime())
     ? String(value)
     : parsedDate.toLocaleString('en-IN', {
      dateStyle: 'medium',
      timeStyle: 'short',
     })
}

function cls(value: any) {
 const status = String(value || 'UNKNOWN').toLowerCase()

 if (
     [
      'success',
      'enabled',
      'completed',
      'operational',
      'available',
     ].includes(status)
 ) {
  return 'success'
 }

 if (
     ['failed', 'failure', 'disabled', 'error'].includes(status)
 ) {
  return 'danger'
 }

 if (
     [
      'created',
      'pending',
      'started',
      'sandbox',
      'processing',
     ].includes(status)
 ) {
  return 'pending'
 }

 return 'neutral'
}

function Status({ value }: { value: any }) {
 return (
     <span className={`status status-${cls(value)}`}>
      <i />
      {String(value || 'UNKNOWN').replace(/_/g, ' ')}
    </span>
 )
}

function Field({
                label,
                children,
               }: {
 label: string
 children: any
}) {
 return (
     <label className="field">
      <span>{label}</span>
      {children}
     </label>
 )
}

function Empty({
                title,
                text,
               }: {
 title: string
 text: string
}) {
 return (
     <div className="empty-state">
      <div className="empty-icon">
       <FileText size={22} />
      </div>
      <strong>{title}</strong>
      <p>{text}</p>
     </div>
 )
}

function Heading({
                  eyebrow,
                  title,
                  description,
                  action,
                 }: {
 eyebrow: string
 title: string
 description: string
 action?: any
}) {
 return (
     <div className="page-heading">
      <div>
       <span className="eyebrow">{eyebrow}</span>
       <h1>{title}</h1>
       <p>{description}</p>
      </div>
      {action}
     </div>
 )
}

function Metric({
                 label,
                 value,
                 sub,
                 icon: Icon,
                 tone,
                }: any) {
 return (
     <div className="metric-card">
      <div className="metric-top">
       <span>{label}</span>
       <div className={`metric-icon ${tone || ''}`}>
        <Icon size={18} />
       </div>
      </div>

      <strong className="metric-value">{value}</strong>

      <div className="metric-sub">
       <span className="metric-dot" />
       {sub}
      </div>
     </div>
 )
}

function Modal({
                title,
                subtitle,
                onClose,
                children,
               }: any) {
 return (
     <div
         className="modal-backdrop"
         onMouseDown={(event) => {
          if (event.target === event.currentTarget) {
           onClose()
          }
         }}
     >
      <div className="modal">
       <div className="modal-header">
        <div>
         <h2>{title}</h2>
         <p>{subtitle}</p>
        </div>

        <button
            className="icon-button"
            type="button"
            onClick={onClose}
            aria-label="Close dialog"
        >
         <X size={18} />
        </button>
       </div>

       {children}
      </div>
     </div>
 )
}

function Table({ rows }: { rows: Row[] }) {
 if (!rows.length) {
  return (
      <Empty
          title="No transactions found"
          text="Create a sandbox transaction or refresh to load backend data."
      />
  )
 }

 return (
     <div className="table-wrap">
      <table>
       <thead>
       <tr>
        <th>Transaction</th>
        <th>Merchant order</th>
        <th>Amount</th>
        <th>Method</th>
        <th>Provider</th>
        <th>Status</th>
        <th>Created</th>
       </tr>
       </thead>

       <tbody>
       {rows.map((transaction, index) => (
           <tr key={transaction.id || index}>
            <td>
             <div className="transaction-ref">
              <div className="transaction-icon">
               <ArrowDownLeft size={15} />
              </div>

              <div>
               <strong>
                {transaction.transactionReference ||
                    `TXN-${transaction.id ?? index}`}
               </strong>
               <span>ID {transaction.id ?? '—'}</span>
              </div>
             </div>
            </td>

            <td className="mono">
             {transaction.merchantOrderId || '—'}
            </td>

            <td className="amount-cell">
             {cash(
                 transaction.amount,
                 transaction.currency || 'INR',
             )}
            </td>

            <td>
                <span className="method-pill">
                  {String(
                      transaction.paymentMethod || '—',
                  ).replace(/_/g, ' ')}
                </span>
            </td>

            <td>
             {transaction.provider?.name ||
                 transaction.provider?.providerName ||
                 transaction.providerName ||
                 (transaction.providerId
                     ? `Provider ${transaction.providerId}`
                     : '—')}
            </td>

            <td>
             <Status value={transaction.status} />
            </td>

            <td>{date(transaction.createdAt)}</td>
           </tr>
       ))}
       </tbody>
      </table>
     </div>
 )
}

export default function App() {
 const [authed, setAuthed] = useState(
     Boolean(sessionStorage.getItem('pratos_access_token')),
 )

 const [page, setPage] = useState<Page>('Overview')
 const [menu, setMenu] = useState(false)

 const [admin, setAdmin] = useState(
     sessionStorage.getItem('pratos_admin_name') ||
     'Administrator',
 )

 const [transactions, setTransactions] = useState<Row[]>([])
 const [attempts, setAttempts] = useState<Row[]>([])
 const [credentials, setCredentials] = useState<Row[]>([])
 const [providers, setProviders] = useState<Row[]>(
     fallbackProviders,
 )

 const [notice, setNotice] = useState('')
 const [toast, setToast] = useState('')
 const [busy, setBusy] = useState(false)
 const [search, setSearch] = useState('')

 const [loginForm, setLoginForm] = useState({
  username: '',
  password: '',
 })

 const [showPass, setShowPass] = useState(false)
 const [createOpen, setCreateOpen] = useState(false)
 const [insights, setInsights] = useState<Row | null>(null)
 const [insightsAt, setInsightsAt] = useState('')
 const [insightsDays, setInsightsDays] = useState(7)
 const [credentialOpen, setCredentialOpen] = useState(false)

 const [txForm, setTxForm] = useState({
  merchantOrderId: '',
  amount: '',
  currency: 'INR',
  paymentMethod: 'UPI',
  environment: 'SANDBOX',
 })

 const [credForm, setCredForm] = useState({
  providerId: '1',
  environment: 'SANDBOX',
  merchantId: '',
  merchantKey: '',
  saltKey: '',
  referenceId: '',
 })

 useEffect(() => {
  if (authed) {
   void loadTransactions()
   void loadCredentials()
   void loadProviders()
   void loadAttempts()
  }
 }, [authed])

 useEffect(() => {
  if (!authed) return

  void loadInsights()

  // Real-time refresh: poll every 10 seconds while the tab is visible.
  const timer = setInterval(() => {
   if (document.hidden) return
   void loadInsights()
   void loadTransactions()
   void loadAttempts()
  }, 10000)

  return () => clearInterval(timer)
 }, [authed, insightsDays])

 // An expired or invalid token (HTTP 401) signs the admin out.
 useEffect(() => {
  if (!authed) return

  const id = api.interceptors.response.use(
      (response) => response,
      (error) => {
       if (error?.response?.status === 401) {
        doLogout()
        setNotice('Your session has expired. Please sign in again.')
       }
       return Promise.reject(error)
      },
  )

  return () => api.interceptors.response.eject(id)
 }, [authed])

 useEffect(() => {
  if (toast) {
   const timer = setTimeout(() => setToast(''), 4000)
   return () => clearTimeout(timer)
  }
 }, [toast])

 async function loadTransactions() {
  try {
   const response = await api.get(routes.transactions, {
    params: { size: 500 },
   })

   setTransactions(
       (unwrap<Row[]>(response.data) as Row[]) || [],
   )

   setNotice('')
  } catch (error) {
   setTransactions([])

   setNotice(
       `Transactions could not be loaded: ${errorMessage(error)}`,
   )
  }
 }

 async function loadInsights() {
  try {
   const response = await api.get(routes.insights, {
    params: { days: insightsDays },
   })

   setInsights((unwrap<Row>(response.data) as Row) || null)
   setInsightsAt(new Date().toLocaleTimeString())
  } catch {
   // Keep the last good snapshot if a refresh fails.
  }
 }

 async function loadCredentials() {
  try {
   const response = await api.get(routes.credentials)

   setCredentials(
       (unwrap<Row[]>(response.data) as Row[]) || [],
   )
  } catch {
   setCredentials([])
  }
 }

 async function loadProviders() {
  try {
   const response = await api.get(routes.providers)

   setProviders(
       (unwrap<Row[]>(response.data) as Row[]) || [],
   )
  } catch {
   setProviders(fallbackProviders)
  }
 }

 async function loadAttempts() {
  try {
   const response = await api.get(routes.attempts)

   setAttempts(
       (unwrap<Row[]>(response.data) as Row[]) || [],
   )
  } catch {
   setAttempts([])
  }
 }

 async function doLogin(event: any) {
  event.preventDefault()

  setBusy(true)
  setNotice('')

  try {
   await login(
       loginForm.username.trim(),
       loginForm.password,
   )

   setAdmin(
       sessionStorage.getItem('pratos_admin_name') ||
       loginForm.username,
   )

   setAuthed(true)
   setToast('Signed in successfully')
  } catch (error) {
   setNotice(errorMessage(error))
  } finally {
   setBusy(false)
  }
 }

 function doLogout() {
  clearSession()
  setAuthed(false)
  setTransactions([])
  setAttempts([])
  setCredentials([])
  setInsights(null)
  setInsightsAt('')
  setLoginForm((form) => ({ ...form, password: '' }))
  setCreateOpen(false)
  setCredentialOpen(false)
  setMenu(false)
  setPage('Overview')
 }

 function confirmLogout() {
  if (window.confirm('Log out of the admin console?')) {
   doLogout()
  }
 }

 async function createTx(event: any) {
  event.preventDefault()
  setBusy(true)

  try {
   await api.post(routes.transactions, {
    merchantOrderId: txForm.merchantOrderId,
    amount: Number(txForm.amount),
    currency: txForm.currency,
    paymentMethod: txForm.paymentMethod,
    environment: txForm.environment,
   })

   setCreateOpen(false)

   setTxForm({
    merchantOrderId: '',
    amount: '',
    currency: 'INR',
    paymentMethod: 'UPI',
    environment: 'SANDBOX',
   })

   setToast('Transaction created')

   await loadTransactions()
  } catch (error) {
   setToast(errorMessage(error))
  } finally {
   setBusy(false)
  }
 }

 async function saveCred(event: any) {
  event.preventDefault()
  setBusy(true)

  try {
   await api.post(routes.credentials, {
    ...credForm,
    providerId: Number(credForm.providerId),
   })

   setCredentialOpen(false)
   setToast('Credentials saved')

   await loadCredentials()
  } catch (error) {
   setToast(errorMessage(error))
  } finally {
   setBusy(false)
  }
 }

 async function attemptAction(
     id: any,
     action: 'process' | 'start' | 'success' | 'failure',
 ) {
  setBusy(true)

  try {
   const url = `${routes.attempts}/${id}/${action}`

   if (action === 'process') {
    await api.post(url)
   } else if (action === 'start') {
    await api.patch(url)
   } else if (action === 'success') {
    await api.patch(url, {
     providerTransactionReference: `DEMO-${Date.now()}`,
    })
   } else {
    await api.patch(url, {
     failureReason:
         'Simulated network error from frontend test',
     failureCategory: 'NETWORK_ERROR',
    })
   }

   setToast(`Attempt ${action} request completed`)

   await loadAttempts()
   await loadTransactions()
  } catch (error) {
   setToast(errorMessage(error))
  } finally {
   setBusy(false)
  }
 }

 const filtered = useMemo(
     () =>
         transactions.filter((transaction) =>
             JSON.stringify(transaction)
                 .toLowerCase()
                 .includes(search.toLowerCase()),
         ),
     [transactions, search],
 )

 const total = transactions.reduce(
     (sum, transaction) =>
         sum + Number(transaction.amount || 0),
     0,
 )

 const success = transactions.filter(
     (transaction) =>
         String(transaction.status).toUpperCase() === 'SUCCESS',
 ).length

 const rate = transactions.length
     ? Math.round((success / transactions.length) * 100)
     : 0

 const daily: Row[] = (insights?.daily as Row[]) || []

 const chartData = daily.map((entry) => ({
  day: new Date(`${entry.date}T00:00:00`).toLocaleDateString('en-IN', {
   day: 'numeric',
   month: 'short',
  }),
  success: Number(entry.success || 0),
  failed: Number(entry.failed || 0),
 }))

 const currencies: Row[] = (insights?.byCurrency as Row[]) || []
 const topCurrency = currencies[0]
 const allTime = insights?.allTime as Row | undefined
 const windowTotals = (insights?.totals || {}) as Row
 const attemptStats = (insights?.attempts || {}) as Row

 if (!authed) {
  return (
      <div className="login-shell">
       <div className="login-visual">
        <div className="brand brand-light">
         <div className="brand-mark">
          <Layers3 size={22} />
         </div>

         <span>
              pratosh<span className="brand-dot">.</span>
            </span>
        </div>

        <div className="visual-copy">
            <span className="eyebrow">
              PAYMENT ORCHESTRATION
            </span>

         <h1>
          One platform.
          <br />
          Every payment path.
         </h1>

         <p>
          Intelligent payment operations, unified visibility,
          and resilient routing — built for modern commerce.
         </p>

         <div className="visual-stats">
          <div>
           <strong>01</strong>
           <span>Unified gateway</span>
          </div>

          <div>
           <strong>02</strong>
           <span>Smart routing</span>
          </div>

          <div>
           <strong>03</strong>
           <span>Retry resilience</span>
          </div>
         </div>
        </div>

        <div className="visual-bottom">
         <span>PRATOS ORCHESTRATION PLATFORM</span>
         <span>SECURE ADMIN ACCESS</span>
        </div>
       </div>

       <div className="login-panel">
        <div className="login-card">
         <div className="mobile-brand brand">
          <div className="brand-mark">
           <Layers3 size={20} />
          </div>

          <span>
                pratosh<span className="brand-dot">.</span>
              </span>
         </div>

         <div className="login-icon">
          <LockKeyhole size={23} />
         </div>

         <span className="eyebrow">ADMIN CONSOLE</span>
         <h2>Welcome back</h2>

         <p className="muted">
          Sign in to manage your payment ecosystem.
         </p>

         <form onSubmit={doLogin} className="login-form">
          <Field label="Username / email">
           <input
               required
               autoComplete="username"
               value={loginForm.username}
               onChange={(event) =>
                   setLoginForm({
                    ...loginForm,
                    username: event.target.value,
                   })
               }
               placeholder="admin@company.com"
           />
          </Field>

          <Field label="Password">
           <div className="password-wrap">
            <input
                required
                autoComplete="current-password"
                type={showPass ? 'text' : 'password'}
                value={loginForm.password}
                onChange={(event) =>
                    setLoginForm({
                     ...loginForm,
                     password: event.target.value,
                    })
                }
                placeholder="Enter your password"
            />

            <button
                type="button"
                className="plain-icon"
                onClick={() => setShowPass(!showPass)}
            >
             {showPass ? 'Hide' : 'Show'}
            </button>
           </div>
          </Field>

          {notice && (
              <div className="error-box">{notice}</div>
          )}

          <button
              className="btn btn-primary btn-wide"
              disabled={busy}
          >
           {busy ? (
               <LoaderCircle
                   className="spin"
                   size={17}
               />
           ) : (
               <LockKeyhole size={17} />
           )}

           Sign in securely
          </button>
         </form>

         <div className="login-secure">
          <ShieldCheck size={16} />
          Protected with backend JWT authentication
         </div>

         <p className="login-foot">
          Admin accounts are provisioned by your backend
          administrator.
         </p>
        </div>
       </div>
      </div>
  )
 }

 return (
     <div className="app-shell">
      {menu && (
          <button
              className="mobile-overlay"
              onClick={() => setMenu(false)}
              aria-label="Close navigation"
          />
      )}

      <aside
          className={`sidebar ${menu ? 'sidebar-open' : ''}`}
      >
       <div className="brand">
        <div className="brand-mark">
         <Layers3 size={21} />
        </div>

        <span>
            pratosh<span className="brand-dot">.</span>
          </span>

        <span className="brand-tag">OPS</span>
       </div>

       <div className="workspace-switch">
        <div className="workspace-avatar">P</div>

        <div>
         <strong>Pratosh Payments</strong>
         <span>Admin workspace</span>
        </div>

        <ChevronDown size={15} />
       </div>

       <nav className="side-nav">
        {['WORKSPACE', 'PAYMENTS', 'OPTIMIZATION'].map(
            (group) => (
                <div className="nav-group" key={group}>
                 <div className="nav-label">{group}</div>

                 {navigation
                     .filter((item) => item.group === group)
                     .map((item) => {
                      const Icon = item.icon

                      return (
                          <button
                              key={item.label}
                              className={`nav-item ${
                                  page === item.label ? 'active' : ''
                              }`}
                              onClick={() => {
                               setPage(item.label)
                               setMenu(false)
                              }}
                          >
                           <Icon size={18} />

                           <span>{item.label}</span>

                           {item.label === 'Payment attempts' &&
                               attempts.length > 0 && (
                                   <span className="nav-count">
                              {attempts.length}
                            </span>
                               )}
                          </button>
                      )
                     })}
                </div>
            ),
        )}
       </nav>

       <div className="sidebar-bottom">
        <div className="help-card">
         <div className="help-icon">
          <CircleHelp size={18} />
         </div>

         <strong>Need a hand?</strong>

         <p>
          Check API connectivity and routing configuration.
         </p>

         <button
             onClick={() =>
                 setToast(
                     'Backend: localhost:8080 · Frontend: localhost:5173',
                 )
             }
         >
          View setup info <ArrowUpRight size={14} />
         </button>
        </div>

        <div className="profile-row">
         <div className="profile-avatar">
          {admin.charAt(0).toUpperCase()}
         </div>

         <div className="profile-info">
          <strong>{admin}</strong>
          <span>Platform administrator</span>
         </div>

         <button
             className="plain-icon"
             title="Log out"
             onClick={doLogout}
         >
          <LogOut size={17} />
         </button>
        </div>
       </div>
      </aside>

      <main className="main-area">
       <header className="topbar">
        <div className="topbar-left">
         <button
             className="mobile-menu plain-icon"
             onClick={() => setMenu(true)}
         >
          <Menu size={21} />
         </button>

         <div className="breadcrumb">
          Workspace <span>/</span> <strong>{page}</strong>
         </div>
        </div>

        <div className="topbar-actions">
         <div className="environment-pill">
          <span />
          SANDBOX ENVIRONMENT
         </div>

         <button
             className="icon-button"
             title="Refresh"
             onClick={() => {
              void loadTransactions()
              void loadAttempts()
              setToast('Data refreshed')
             }}
         >
          <RefreshCw size={17} />
         </button>

         <button
             className="icon-button notification"
             title="Notifications"
             onClick={() => setToast('No new alerts')}
         >
          <Bell size={17} />
          <i />
         </button>

         <div className="top-avatar" title={admin}>
          {admin.charAt(0).toUpperCase()}
         </div>

         <button
             className="btn btn-secondary btn-sm"
             title="Log out"
             onClick={confirmLogout}
         >
          <LogOut size={15} />
          Log out
         </button>
        </div>
       </header>

       <div className="page-content">
        {notice && (
            <div className="notice-banner">
             <Activity size={17} />
             <span>{notice}</span>

             <button
                 className="plain-icon"
                 onClick={() => setNotice('')}
             >
              <X size={16} />
             </button>
            </div>
        )}

        {page === 'Overview' && (
            <section>
             <div className="welcome-row">
              <div>
                  <span className="eyebrow">
                    MONITORING CENTER
                  </span>

               <h1>
                Payment overview <span className="wave">✦</span>
               </h1>

               <p>
                Monitor payment activity across your
                orchestration infrastructure.
               </p>
              </div>

              <div className="welcome-actions">
                  <span className="live-pill">
                    <i /> LIVE WORKSPACE
                  </span>

               <button
                   className="btn btn-primary"
                   onClick={() => setCreateOpen(true)}
               >
                <Plus size={17} />
                New transaction
               </button>
              </div>
             </div>

             <div className="metrics-grid">
              <Metric
                  label="Total transactions"
                  value={allTime ? allTime.transactions : transactions.length}
                  sub="All time, live from the backend"
                  icon={ArrowLeftRight}
              />

              <Metric
                  label="Recorded volume"
                  value={
                   topCurrency
                       ? cash(topCurrency.totalAmount, topCurrency.currency)
                       : cash(total)
                  }
                  sub={
                   currencies.length > 1
                       ? `${topCurrency.currency} + ${currencies.length - 1} more, last ${insightsDays} days`
                       : `Last ${insightsDays} days`
                  }
                  icon={WalletCards}
                  tone="blue"
              />

              <Metric
                  label="Success rate"
                  value={`${allTime ? pct(allTime.successRatePercent) : rate}%`}
                  sub="Successful ÷ finished payments"
                  icon={Activity}
                  tone="green"
              />

              <Metric
                  label="Credential profiles"
                  value={credentials.length}
                  sub="Profiles returned by API"
                  icon={KeyRound}
                  tone="purple"
              />
             </div>

             <div className="overview-grid">
              <div className="data-card">
               <div className="card-title-row">
                <div>
                 <h3>Payment activity</h3>
                 <p>Successful payments per day</p>
                </div>

                <span className="tag">LIVE</span>
               </div>

               <div className="chart-area">
                <ResponsiveContainer
                    width="100%"
                    height="100%"
                >
                 <AreaChart
                     data={chartData}
                     margin={{
                      top: 12,
                      right: 10,
                      left: -16,
                      bottom: 0,
                     }}
                 >
                  <defs>
                   <linearGradient
                       id="fill"
                       x1="0"
                       y1="0"
                       x2="0"
                       y2="1"
                   >
                    <stop
                        offset="0%"
                        stopColor="#5a6cf5"
                        stopOpacity={0.22}
                    />
                    <stop
                        offset="95%"
                        stopColor="#5a6cf5"
                        stopOpacity={0}
                    />
                   </linearGradient>
                  </defs>

                  <CartesianGrid
                      strokeDasharray="3 5"
                      vertical={false}
                      stroke="#edf0f6"
                  />

                  <XAxis
                      dataKey="day"
                      axisLine={false}
                      tickLine={false}
                      tick={{
                       fill: '#8c95a7',
                       fontSize: 12,
                      }}
                      dy={10}
                  />

                  <YAxis
                      axisLine={false}
                      tickLine={false}
                      tick={{
                       fill: '#8c95a7',
                       fontSize: 11,
                      }}
                      allowDecimals={false}
                      tickFormatter={(value) => compact(value)}
                  />

                  <Tooltip
                      formatter={(value: any) => `${value} successful`}
                      contentStyle={{
                       borderRadius: 12,
                       border: '1px solid #e9edf5',
                      }}
                  />

                  <Area
                      type="monotone"
                      dataKey="success"
                      stroke="#5a6cf5"
                      strokeWidth={2.5}
                      fill="url(#fill)"
                  />
                 </AreaChart>
                </ResponsiveContainer>
               </div>

               <div className="chart-footer">
                    <span>
                      <i className="legend-dot" />
                      Live data · refreshes every 10 seconds
                    </span>

                <span>Last {insightsDays} days</span>
               </div>
              </div>

              <div className="data-card routing-card">
               <div className="card-title-row">
                <div>
                 <h3>Routing health</h3>
                 <p>Configuration snapshot</p>
                </div>

                <div className="round-icon">
                 <Zap size={18} />
                </div>
               </div>

               <div className="route-health">
                <div className="route-health-icon">
                 <Check size={18} />
                </div>

                <div>
                 <strong>Rule-based routing</strong>
                 <span>Backend routing service</span>
                </div>

                <Status value="Configured" />
               </div>

               <div className="route-row">
                <span>Credential profiles</span>
                <strong>{credentials.length}</strong>
               </div>

               <div className="route-row">
                <span>Loaded transactions</span>
                <strong>{transactions.length}</strong>
               </div>

               <div className="route-row">
                <span>Retry policy</span>
                <Status value="Configured" />
               </div>

               <button
                   className="text-link"
                   onClick={() => setPage('Smart routing')}
               >
                View routing rules <ArrowUpRight size={15} />
               </button>
              </div>
             </div>

             <div className="data-card recent-card">
              <div className="card-title-row">
               <div>
                <h3>Recent transactions</h3>
                <p>
                 Latest payment activity from the backend
                </p>
               </div>

               <button
                   className="text-link"
                   onClick={() => setPage('Transactions')}
               >
                View all <ArrowUpRight size={15} />
               </button>
              </div>

              <Table rows={transactions.slice(0, 5)} />
             </div>
            </section>
        )}

        {page === 'Transactions' && (
            <section>
             <Heading
                 eyebrow="PAYMENT OPERATIONS"
                 title="Transactions"
                 description="Track payments and create sandbox transactions."
                 action={
                  <button
                      className="btn btn-primary"
                      onClick={() => setCreateOpen(true)}
                  >
                   <Plus size={17} />
                   Create transaction
                  </button>
                 }
             />

             <div className="table-toolbar">
              <div className="search-box">
               <Search size={17} />

               <input
                   placeholder="Search order, reference, status…"
                   value={search}
                   onChange={(event) =>
                       setSearch(event.target.value)
                   }
               />
              </div>

              <button
                  className="btn btn-secondary"
                  onClick={() => void loadTransactions()}
              >
               <RefreshCw size={15} />
               Refresh
              </button>
             </div>

             <Table rows={filtered} />
            </section>
        )}

        {page === 'Gateways' && (
            <section>
             <Heading
                 eyebrow="PROVIDER ECOSYSTEM"
                 title="Payment gateways"
                 description="Providers available to the orchestration layer."
                 action={
                  <button
                      className="btn btn-secondary"
                      onClick={() => void loadProviders()}
                  >
                   <RefreshCw size={16} />
                   Refresh
                  </button>
                 }
             />

             <div className="gateway-grid">
              {providers.map((provider: any) => (
                  <div className="gateway-card" key={provider.id}>
                   <div className="gateway-top">
                    <div className="provider-logo">
                     {String(
                         provider.name || provider.code || 'P',
                     ).slice(0, 1)}
                    </div>

                    <Status
                        value={
                            provider.status ||
                            (provider.enabled
                                ? 'Enabled'
                                : 'Sandbox')
                        }
                    />
                   </div>

                   <h3>
                    {provider.name ||
                        provider.providerName ||
                        provider.code ||
                        `Provider ${provider.id}`}
                   </h3>

                   <p className="muted">
                    {provider.code ||
                        provider.providerCode ||
                        'Payment service provider'}
                   </p>

                   <div className="gateway-divider" />

                   <div className="gateway-meta">
                    <span>Methods</span>
                    <strong>
                     {provider.methods ||
                         provider.supportedMethods ||
                         'Configured in backend'}
                    </strong>
                   </div>

                   <div className="gateway-meta">
                    <span>Health</span>
                    <Status
                        value={provider.health || 'Not verified'}
                    />
                   </div>

                   <button
                       className="btn btn-secondary btn-wide"
                       onClick={() => {
                        setPage('Credentials')
                        setCredForm({
                         ...credForm,
                         providerId: String(provider.id),
                        })
                       }}
                   >
                    Manage credentials <ArrowUpRight size={15} />
                   </button>
                  </div>
              ))}
             </div>

             <div className="info-strip">
              <ShieldCheck size={19} />

              <div>
               <strong>Secure credential handling</strong>
               <p>
                Use sandbox credentials. Secret encryption is
                handled by the backend.
               </p>
              </div>
             </div>
            </section>
        )}

        {page === 'Credentials' && (
            <section>
             <Heading
                 eyebrow="SECURE CONFIGURATION"
                 title="Provider credentials"
                 description="Configure merchant identities and sandbox access."
                 action={
                  <button
                      className="btn btn-primary"
                      onClick={() => setCredentialOpen(true)}
                  >
                   <Plus size={17} />
                   Add credentials
                  </button>
                 }
             />

             <div className="security-callout">
              <div className="callout-icon">
               <KeyRound size={20} />
              </div>

              <div>
               <strong>Credential vault</strong>
               <p>
                Secret values are submitted to the backend;
                saved secrets are not displayed.
               </p>
              </div>

              <ShieldCheck size={20} />
             </div>

             <div className="data-card">
              <div className="card-title-row">
               <div>
                <h3>Configured credentials</h3>
                <p>Provider and environment access</p>
               </div>

               <button
                   className="btn btn-secondary btn-sm"
                   onClick={() => void loadCredentials()}
               >
                <RefreshCw size={14} />
                Refresh
               </button>
              </div>

              {credentials.length ? (
                  <div className="table-wrap">
                   <table>
                    <thead>
                    <tr>
                     <th>Provider</th>
                     <th>MID</th>
                     <th>Environment</th>
                     <th>Reference ID</th>
                     <th>Status</th>
                    </tr>
                    </thead>

                    <tbody>
                    {credentials.map(
                        (credential: any, index: number) => (
                            <tr key={credential.id || index}>
                             <td>
                              Provider{' '}
                              {credential.providerId ??
                                  credential.provider?.id ??
                                  '—'}
                             </td>

                             <td className="mono">
                              {credential.merchantId || '—'}
                             </td>

                             <td>
                              {credential.environment || '—'}
                             </td>

                             <td className="mono">
                              {credential.referenceId || '—'}
                             </td>

                             <td>
                              <Status
                                  value={
                                   credential.enabled === false
                                       ? 'Disabled'
                                       : 'Enabled'
                                  }
                              />
                             </td>
                            </tr>
                        ),
                    )}
                    </tbody>
                   </table>
                  </div>
              ) : (
                  <Empty
                      title="No credential rows returned"
                      text="You can still submit a credential profile. If the GET route is not implemented, the list remains empty until a matching backend endpoint is available."
                  />
              )}
             </div>
            </section>
        )}

        {page === 'Payment attempts' && (
            <section>
             <Heading
                 eyebrow="RELIABILITY & FAILOVER"
                 title="Payment attempts"
                 description="Inspect attempt records and test lifecycle actions."
                 action={
                  <button
                      className="btn btn-secondary"
                      onClick={() => void loadAttempts()}
                  >
                   <RefreshCw size={16} />
                   Refresh
                  </button>
                 }
             />

             <div className="info-strip warning">
              <Activity size={19} />

              <div>
               <strong>Sandbox test actions</strong>
               <p>
                Start, success and failure actions call backend
                endpoints. Do not use these simulation actions
                for live payment processing.
               </p>
              </div>
             </div>

             <div className="data-card">
              <div className="card-title-row">
               <div>
                <h3>Attempt history</h3>
                <p>{attempts.length} rows returned</p>
               </div>
              </div>

              {attempts.length ? (
                  <div className="table-wrap">
                   <table>
                    <thead>
                    <tr>
                     <th>Attempt</th>
                     <th>Transaction</th>
                     <th>Provider</th>
                     <th>Status</th>
                     <th>Failure category</th>
                     <th>Created</th>
                     <th>Actions</th>
                    </tr>
                    </thead>

                    <tbody>
                    {attempts.map(
                        (attempt: any, index: number) => (
                            <tr key={attempt.id || index}>
                             <td className="mono">
                              #
                              {attempt.attemptNumber ??
                                  attempt.id}
                             </td>

                             <td>
                              {attempt.transaction
                                      ?.transactionReference ||
                                  attempt.transactionId ||
                                  attempt.transaction?.id ||
                                  '—'}
                             </td>

                             <td>
                              {attempt.provider?.name ||
                                  `Provider ${
                                      attempt.provider?.id ??
                                      attempt.providerId ??
                                      '—'
                                  }`}
                             </td>

                             <td>
                              <Status value={attempt.status} />
                             </td>

                             <td>
                              {attempt.failureCategory || '—'}
                             </td>

                             <td>{date(attempt.createdAt)}</td>

                             <td>
                              <div className="row-actions">
                               <button
                                   disabled={busy}
                                   onClick={() =>
                                       void attemptAction(
                                           attempt.id,
                                           'process',
                                       )
                                   }
                               >
                                Process
                               </button>

                               <button
                                   disabled={busy}
                                   onClick={() =>
                                       void attemptAction(
                                           attempt.id,
                                           'start',
                                       )
                                   }
                               >
                                Start
                               </button>

                               <button
                                   disabled={busy}
                                   onClick={() =>
                                       void attemptAction(
                                           attempt.id,
                                           'success',
                                       )
                                   }
                               >
                                Success
                               </button>

                               <button
                                   disabled={busy}
                                   onClick={() =>
                                       void attemptAction(
                                           attempt.id,
                                           'failure',
                                       )
                                   }
                               >
                                Fail
                               </button>
                              </div>
                             </td>
                            </tr>
                        ),
                    )}
                    </tbody>
                   </table>
                  </div>
              ) : (
                  <Empty
                      title="No attempts loaded"
                      text="Your backend may not expose a list endpoint yet. The API path is configured in src/api.ts."
                  />
              )}
             </div>
            </section>
        )}

        {page === 'Smart routing' && (
            <section>
             <Heading
                 eyebrow="ORCHESTRATION ENGINE"
                 title="Smart routing"
                 description="The current provider selection strategy and failover conditions."
             />

             <div className="routing-hero">
              <div className="routing-orb">
               <Zap size={30} />
              </div>

              <div>
               <span className="eyebrow">ROUTING ENGINE</span>
               <h2>Route with intent.</h2>

               <p>
                Provider selection is based on payment method,
                currency, environment, enabled status,
                credential availability, and configured priority.
               </p>
              </div>
             </div>

             <div className="routing-grid">
              {[
               {
                icon: CreditCard,
                title: 'Payment method',
                text: 'Only providers configured for the requested method should qualify.',
               },
               {
                icon: WalletCards,
                title: 'Currency support',
                text: 'Currency support filters providers to the transaction currency.',
               },
               {
                icon: KeyRound,
                title: 'Credential readiness',
                text: 'Enabled credentials must exist for the requested environment.',
               },
               {
                icon: RefreshCw,
                title: 'Retry and failover',
                text: 'Current policy includes network errors, provider unavailable, and temporary provider errors.',
               },
              ].map((item) => {
               const Icon = item.icon

               return (
                   <div className="data-card" key={item.title}>
                    <div className="feature-icon">
                     <Icon size={19} />
                    </div>

                    <h3>{item.title}</h3>
                    <p>{item.text}</p>
                   </div>
               )
              })}
             </div>

             <div className="info-strip">
              <Sparkles size={19} />

              <div>
               <strong>
                AI routing is a future enhancement
               </strong>

               <p>
                Collect reliable provider latency and success
                metrics before adding score-based or AI-driven
                routing decisions.
               </p>
              </div>
             </div>
            </section>
        )}

        {page === 'Reports' && (
            <section>
             <Heading
                 eyebrow="FINANCIAL OPERATIONS"
                 title="Reports & reconciliation"
                 description="Summary of loaded transactions and reconciliation readiness."
                 action={
                  <button
                      className="btn btn-secondary"
                      onClick={() => void loadTransactions()}
                  >
                   <RefreshCw size={16} />
                   Refresh
                  </button>
                 }
             />

             <div className="metrics-grid">
              <Metric
                  label="Transactions"
                  value={transactions.length}
                  sub="Loaded from backend"
                  icon={ArrowLeftRight}
              />

              <Metric
                  label="Successful"
                  value={success}
                  sub="Status equals SUCCESS"
                  icon={Check}
                  tone="green"
              />

              <Metric
                  label="Recorded volume"
                  value={cash(total)}
                  sub="Loaded transaction amounts"
                  icon={WalletCards}
              />

              <Metric
                  label="Credential profiles"
                  value={credentials.length}
                  sub="Loaded profiles"
                  icon={KeyRound}
                  tone="purple"
              />
             </div>

             <div className="card-title-row" style={{ marginTop: 8 }}>
              <div>
               <h3>Live insights</h3>
               <p>
                Real-time breakdown by currency, provider, payment
                method and outcome
               </p>
              </div>

              <div className="welcome-actions">
               <span className="live-pill">
                <i /> LIVE · {insightsAt || 'loading'}
               </span>

               <select
                   className="btn btn-secondary"
                   value={insightsDays}
                   onChange={(event) =>
                       setInsightsDays(Number(event.target.value))
                   }
               >
                <option value={1}>Today</option>
                <option value={7}>Last 7 days</option>
                <option value={30}>Last 30 days</option>
                <option value={90}>Last 90 days</option>
               </select>
              </div>
             </div>

             <div className="metrics-grid">
              <Metric
                  label="Payments"
                  value={windowTotals.transactions ?? 0}
                  sub={`Last ${insightsDays} days`}
                  icon={ArrowLeftRight}
              />

              <Metric
                  label="Success rate"
                  value={`${pct(windowTotals.successRatePercent)}%`}
                  sub={`${windowTotals.success ?? 0} successful · ${windowTotals.failed ?? 0} failed`}
                  icon={Activity}
                  tone="green"
              />

              <Metric
                  label="In flight"
                  value={windowTotals.pending ?? 0}
                  sub="Awaiting processing or verification"
                  icon={RefreshCw}
                  tone="blue"
              />

              <Metric
                  label="Avg processing time"
                  value={`${attemptStats.averageProcessingMs ?? 0} ms`}
                  sub={`${attemptStats.total ?? 0} attempts · ${attemptStats.retriedTransactions ?? 0} retried`}
                  icon={Zap}
                  tone="purple"
              />
             </div>

             <div className="data-card" style={{ marginBottom: 18 }}>
              <div className="card-title-row">
               <div>
                <h3>Outcomes per day</h3>
                <p>Successful and failed payments</p>
               </div>

               <span className="tag">LIVE</span>
              </div>

              <div className="chart-area">
               <ResponsiveContainer width="100%" height="100%">
                <BarChart
                    data={chartData}
                    margin={{ top: 8, right: 10, left: -16, bottom: 0 }}
                >
                 <CartesianGrid
                     strokeDasharray="3 5"
                     vertical={false}
                     stroke="#edf0f6"
                 />
                 <XAxis
                     dataKey="day"
                     axisLine={false}
                     tickLine={false}
                     tick={{ fill: '#8c95a7', fontSize: 11 }}
                 />
                 <YAxis
                     allowDecimals={false}
                     axisLine={false}
                     tickLine={false}
                     tick={{ fill: '#8c95a7', fontSize: 11 }}
                 />
                 <Tooltip
                     contentStyle={{
                      borderRadius: 12,
                      border: '1px solid #e9edf5',
                     }}
                 />
                 <Legend />
                 <Bar
                     dataKey="success"
                     name="Successful"
                     fill="#25ae80"
                     radius={[4, 4, 0, 0]}
                 />
                 <Bar
                     dataKey="failed"
                     name="Failed"
                     fill="#e5636f"
                     radius={[4, 4, 0, 0]}
                 />
                </BarChart>
               </ResponsiveContainer>
              </div>
             </div>

             <div className="data-card" style={{ marginBottom: 18 }}>
              <div className="card-title-row">
               <div>
                <h3>By currency</h3>
                <p>Amounts are never added across currencies</p>
               </div>
              </div>

              <MiniTable
                  empty="No payments in this period."
                  rows={currencies}
                  columns={[
                   { label: 'Currency', render: (r) => r.currency },
                   { label: 'Payments', render: (r) => r.transactions },
                   { label: 'Success', render: (r) => r.success },
                   { label: 'Failed', render: (r) => r.failed },
                   { label: 'Pending', render: (r) => r.pending },
                   {
                    label: 'Total amount',
                    render: (r) => cash(r.totalAmount, r.currency),
                   },
                   {
                    label: 'Successful amount',
                    render: (r) => cash(r.successAmount, r.currency),
                   },
                   {
                    label: 'Failed amount',
                    render: (r) => cash(r.failedAmount, r.currency),
                   },
                   {
                    label: 'Avg ticket',
                    render: (r) => cash(r.averageAmount, r.currency),
                   },
                   {
                    label: 'Success rate',
                    render: (r) => `${pct(r.successRatePercent)}%`,
                   },
                  ]}
              />
             </div>

             <div
                 style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))',
                  gap: 18,
                  marginBottom: 18,
                 }}
             >
              <div className="data-card">
               <div className="card-title-row">
                <div>
                 <h3>By provider</h3>
                 <p>Routing outcomes per gateway</p>
                </div>
               </div>

               <MiniTable
                   empty="No payments in this period."
                   rows={(insights?.byProvider as Row[]) || []}
                   columns={[
                    { label: 'Provider', render: (r) => r.provider },
                    { label: 'Payments', render: (r) => r.transactions },
                    { label: 'Success', render: (r) => r.success },
                    { label: 'Failed', render: (r) => r.failed },
                    {
                     label: 'Rate',
                     render: (r) => `${pct(r.successRatePercent)}%`,
                    },
                    {
                     label: 'Successful volume',
                     render: (r) =>
                         volumeByCurrency(r.successAmountByCurrency),
                    },
                   ]}
               />
              </div>

              <div className="data-card">
               <div className="card-title-row">
                <div>
                 <h3>By payment method</h3>
                 <p>UPI, cards, bank transfer and QR</p>
                </div>
               </div>

               <MiniTable
                   empty="No payments in this period."
                   rows={(insights?.byPaymentMethod as Row[]) || []}
                   columns={[
                    {
                     label: 'Method',
                     render: (r) =>
                         String(r.paymentMethod).replace(/_/g, ' '),
                    },
                    { label: 'Payments', render: (r) => r.transactions },
                    { label: 'Success', render: (r) => r.success },
                    { label: 'Failed', render: (r) => r.failed },
                    {
                     label: 'Rate',
                     render: (r) => `${pct(r.successRatePercent)}%`,
                    },
                    {
                     label: 'Successful volume',
                     render: (r) =>
                         volumeByCurrency(r.successAmountByCurrency),
                    },
                   ]}
               />
              </div>

              <div className="data-card">
               <div className="card-title-row">
                <div>
                 <h3>Failure reasons</h3>
                 <p>Why attempts failed</p>
                </div>
               </div>

               <MiniTable
                   empty="No failed attempts in this period."
                   rows={(attemptStats.failureCategories as Row[]) || []}
                   columns={[
                    {
                     label: 'Category',
                     render: (r) =>
                         String(r.category).replace(/_/g, ' '),
                    },
                    { label: 'Attempts', render: (r) => r.count },
                    {
                     label: 'Share',
                     render: (r) => `${pct(r.sharePercent)}%`,
                    },
                   ]}
               />
              </div>

              <div className="data-card">
               <div className="card-title-row">
                <div>
                 <h3>Attempts and retries</h3>
                 <p>How much work each payment takes</p>
                </div>
               </div>

               <div className="route-row">
                <span>Total attempts</span>
                <strong>{attemptStats.total ?? 0}</strong>
               </div>
               <div className="route-row">
                <span>Successful attempts</span>
                <strong>{attemptStats.success ?? 0}</strong>
               </div>
               <div className="route-row">
                <span>Failed attempts</span>
                <strong>{attemptStats.failed ?? 0}</strong>
               </div>
               <div className="route-row">
                <span>Payments that needed a retry</span>
                <strong>{attemptStats.retriedTransactions ?? 0}</strong>
               </div>
               <div className="route-row">
                <span>Average attempts per payment</span>
                <strong>
                 {attemptStats.averageAttemptsPerTransaction ?? 0}
                </strong>
               </div>
              </div>
             </div>

             <div className="data-card">
              <div className="card-title-row">
               <div>
                <h3>Reconciliation readiness</h3>
                <p>Matching transaction and provider records</p>
               </div>

               <span className="tag">DEVELOPMENT</span>
              </div>

              <div className="checklist">
               <div>
                <Check size={17} />
                <span>Internal transaction reference</span>
                <Status value="Available" />
               </div>

               <div>
                <Check size={17} />
                <span>Merchant order identifier</span>
                <Status value="Available" />
               </div>

               <div>
                <Activity size={17} />
                <span>Provider transaction reference</span>
                <Status value="Depends on attempt result" />
               </div>

               <div>
                <Activity size={17} />
                <span>
                      Settlement file / provider API matching
                    </span>
                <Status value="Not implemented" />
               </div>
              </div>

              <p className="footnote">
               This is a development summary, not a settlement
               or bank reconciliation report.
              </p>
             </div>
            </section>
        )}
       </div>
      </main>

      {createOpen && (
          <Modal
              title="Create payment transaction"
              subtitle="Create a transaction in your sandbox environment."
              onClose={() => setCreateOpen(false)}
          >
           <form className="modal-form" onSubmit={createTx}>
            <Field label="Merchant order ID">
             <input
                 required
                 value={txForm.merchantOrderId}
                 onChange={(event) =>
                     setTxForm({
                      ...txForm,
                      merchantOrderId: event.target.value,
                     })
                 }
                 placeholder="ORDER-2026-0001"
             />
            </Field>

            <div className="form-grid">
             <Field label="Amount">
              <input
                  required
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={txForm.amount}
                  onChange={(event) =>
                      setTxForm({
                       ...txForm,
                       amount: event.target.value,
                      })
                  }
                  placeholder="1000.00"
              />
             </Field>

             <Field label="Currency">
              <select
                  value={txForm.currency}
                  onChange={(event) =>
                      setTxForm({
                       ...txForm,
                       currency: event.target.value,
                      })
                  }
              >
               <option>INR</option>
               <option>USD</option>
               <option>EUR</option>
               <option>GBP</option>
              </select>
             </Field>
            </div>

            <div className="form-grid">
             <Field label="Payment method">
              <select
                  value={txForm.paymentMethod}
                  onChange={(event) =>
                      setTxForm({
                       ...txForm,
                       paymentMethod: event.target.value,
                      })
                  }
              >
               <option>UPI</option>
               <option>CREDIT_CARD</option>
               <option>DEBIT_CARD</option>
               <option>NET_BANKING</option>
               <option>QR_CODE</option>
               <option>BANK_TRANSFER</option>
              </select>
             </Field>

             <Field label="Environment">
              <select
                  value={txForm.environment}
                  onChange={(event) =>
                      setTxForm({
                       ...txForm,
                       environment: event.target.value,
                      })
                  }
              >
               <option>SANDBOX</option>
               <option>PRODUCTION</option>
              </select>
             </Field>
            </div>

            <div className="modal-actions">
             <button
                 type="button"
                 className="btn btn-secondary"
                 onClick={() => setCreateOpen(false)}
             >
              Cancel
             </button>

             <button
                 disabled={busy}
                 className="btn btn-primary"
             >
              {busy ? (
                  <LoaderCircle
                      className="spin"
                      size={16}
                  />
              ) : (
                  <Plus size={16} />
              )}

              Create transaction
             </button>
            </div>
           </form>
          </Modal>
      )}

      {credentialOpen && (
          <Modal
              title="Add provider credentials"
              subtitle="Use sandbox credentials while testing."
              onClose={() => setCredentialOpen(false)}
          >
           <form className="modal-form" onSubmit={saveCred}>
            <Field label="Provider">
             <select
                 value={credForm.providerId}
                 onChange={(event) =>
                     setCredForm({
                      ...credForm,
                      providerId: event.target.value,
                     })
                 }
             >
              {providers.map((provider: any) => (
                  <option
                      key={provider.id}
                      value={provider.id}
                  >
                   {provider.name ||
                       provider.providerName ||
                       provider.code}{' '}
                   (ID {provider.id})
                  </option>
              ))}
             </select>
            </Field>

            <div className="form-grid">
             <Field label="Environment">
              <select
                  value={credForm.environment}
                  onChange={(event) =>
                      setCredForm({
                       ...credForm,
                       environment: event.target.value,
                      })
                  }
              >
               <option>SANDBOX</option>
               <option>PRODUCTION</option>
              </select>
             </Field>

             <Field label="Merchant ID (MID)">
              <input
                  required
                  value={credForm.merchantId}
                  onChange={(event) =>
                      setCredForm({
                       ...credForm,
                       merchantId: event.target.value,
                      })
                  }
                  placeholder="PSP merchant ID"
              />
             </Field>
            </div>

            <Field label="Merchant key">
             <input
                 required
                 autoComplete="off"
                 value={credForm.merchantKey}
                 onChange={(event) =>
                     setCredForm({
                      ...credForm,
                      merchantKey: event.target.value,
                     })
                 }
                 placeholder="Sandbox merchant key"
             />
            </Field>

            <Field label="Salt key">
             <input
                 required
                 autoComplete="off"
                 value={credForm.saltKey}
                 onChange={(event) =>
                     setCredForm({
                      ...credForm,
                      saltKey: event.target.value,
                     })
                 }
                 placeholder="Sandbox salt key"
             />
            </Field>

            <Field label="Reference ID">
             <input
                 required
                 value={credForm.referenceId}
                 onChange={(event) =>
                     setCredForm({
                      ...credForm,
                      referenceId: event.target.value,
                     })
                 }
                 placeholder="Merchant reference ID"
             />
            </Field>

            <div className="security-note">
             <LockKeyhole size={15} />
             Keys are sent to your backend; ensure server-side
             encryption is configured.
            </div>

            <div className="modal-actions">
             <button
                 type="button"
                 className="btn btn-secondary"
                 onClick={() => setCredentialOpen(false)}
             >
              Cancel
             </button>

             <button
                 disabled={busy}
                 className="btn btn-primary"
             >
              {busy ? (
                  <LoaderCircle
                      className="spin"
                      size={16}
                  />
              ) : (
                  <Check size={16} />
              )}

              Save credentials
             </button>
            </div>
           </form>
          </Modal>
      )}

      {toast && (
          <div className="toast">
           <Check size={17} />
           <span>{toast}</span>

           <button
               className="plain-icon"
               onClick={() => setToast('')}
               aria-label="Dismiss notification"
           >
            <X size={15} />
           </button>
          </div>
      )}
     </div>
 )
}