import { Suspense } from "react"
import { AuthForm } from "@/components/auth-form"

export default function Page() {
  // useSearchParams (redirect/motivo) exige Suspense no build do Next 14
  return (
    <Suspense>
      <AuthForm modo="login" />
    </Suspense>
  )
}
