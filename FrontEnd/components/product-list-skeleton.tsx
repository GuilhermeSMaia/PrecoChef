import { Skeleton } from "@/components/ui/skeleton"
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card"

export function ProductListSkeleton() {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
      {Array.from({ length: 8 }).map((_, i) => (
        <Card key={i} className="overflow-hidden animate-pulse">
          <CardHeader className="pb-3">
            <div className="flex items-start justify-between">
              <div className="flex-1 space-y-2">
                <Skeleton className="h-5 w-3/4" />
                <div className="flex items-center gap-2">
                  <Skeleton className="h-4 w-16 rounded-full" />
                  <Skeleton className="h-3 w-8" />
                </div>
              </div>
              <Skeleton className="h-6 w-12 rounded-full" />
            </div>
          </CardHeader>

          <CardContent className="space-y-4">
            <div className="p-4 rounded-xl border">
              <div className="flex items-center justify-between mb-2">
                <Skeleton className="h-5 w-20 rounded-full" />
                <Skeleton className="h-6 w-16" />
              </div>
              <Skeleton className="h-4 w-32" />
              <Skeleton className="h-3 w-24 mt-1" />
            </div>

            <div className="space-y-2">
              <Skeleton className="h-4 w-24" />
              <div className="space-y-1">
                <div className="flex justify-between p-2 rounded-lg">
                  <Skeleton className="h-4 w-20" />
                  <Skeleton className="h-4 w-16" />
                </div>
                <div className="flex justify-between p-2 rounded-lg">
                  <Skeleton className="h-4 w-24" />
                  <Skeleton className="h-4 w-16" />
                </div>
              </div>
            </div>
          </CardContent>

          <CardFooter className="pt-0 flex gap-2">
            <Skeleton className="h-10 flex-1" />
            <Skeleton className="h-10 w-10" />
          </CardFooter>
        </Card>
      ))}
    </div>
  )
}
