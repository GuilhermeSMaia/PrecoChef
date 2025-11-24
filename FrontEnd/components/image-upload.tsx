"use client"

import type React from "react"

import { useState, useRef } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Upload, X, ImageIcon } from "lucide-react"
import { useToast } from "@/components/ui/use-toast"

interface ImageUploadProps {
  value?: string
  onChange: (value: string) => void
  placeholder?: string
}

export function ImageUpload({ value, onChange, placeholder = "Adicionar imagem" }: ImageUploadProps) {
  const [isDragging, setIsDragging] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const { toast } = useToast()

  const handleFileSelect = async (file: File) => {
    if (!file.type.startsWith("image/")) {
      toast({
        title: "Arquivo inválido",
        description: "Por favor, selecione apenas arquivos de imagem.",
        variant: "destructive",
      })
      return
    }

    if (file.size > 5 * 1024 * 1024) {
      // 5MB limit
      toast({
        title: "Arquivo muito grande",
        description: "A imagem deve ter no máximo 5MB.",
        variant: "destructive",
      })
      return
    }

    setIsUploading(true)

    try {
      // Simulate upload delay
      await new Promise((resolve) => setTimeout(resolve, 1000))

      // Convert to base64 for demo purposes
      // In a real app, you would upload to a cloud service
      const reader = new FileReader()
      reader.onload = (e) => {
        const result = e.target?.result as string
        onChange(result)
        setIsUploading(false)
        toast({
          title: "Imagem carregada",
          description: "A imagem foi carregada com sucesso.",
        })
      }
      reader.readAsDataURL(file)
    } catch (error) {
      setIsUploading(false)
      toast({
        title: "Erro no upload",
        description: "Não foi possível carregar a imagem. Tente novamente.",
        variant: "destructive",
      })
    }
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)

    const files = Array.from(e.dataTransfer.files)
    if (files.length > 0) {
      handleFileSelect(files[0])
    }
  }

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(true)
  }

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
  }

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || [])
    if (files.length > 0) {
      handleFileSelect(files[0])
    }
  }

  const removeImage = () => {
    onChange("")
    if (fileInputRef.current) {
      fileInputRef.current.value = ""
    }
  }

  if (value) {
    return (
      <Card className="border-2 border-dashed theme-transition">
        <CardContent className="p-4">
          <div className="relative group">
            <img src={value || "/placeholder.svg"} alt="Preview" className="w-full h-48 object-cover rounded-lg" />
            <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity rounded-lg flex items-center justify-center">
              <Button variant="destructive" size="sm" onClick={removeImage} className="gap-2">
                <X className="h-4 w-4" />
                Remover
              </Button>
            </div>
          </div>
          <p className="text-sm text-muted-foreground mt-2 text-center">Clique na imagem para alterar</p>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card
      className={`border-2 border-dashed transition-all theme-transition cursor-pointer ${
        isDragging ? "border-primary bg-primary/5" : "border-muted-foreground/25 hover:border-primary/50"
      }`}
      onDrop={handleDrop}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onClick={() => fileInputRef.current?.click()}
    >
      <CardContent className="p-8">
        <div className="flex flex-col items-center justify-center text-center space-y-4">
          {isUploading ? (
            <>
              <div className="h-12 w-12 rounded-full border-4 border-primary border-t-transparent animate-spin" />
              <p className="text-sm text-muted-foreground">Carregando imagem...</p>
            </>
          ) : (
            <>
              <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
                {isDragging ? (
                  <Upload className="h-6 w-6 text-primary" />
                ) : (
                  <ImageIcon className="h-6 w-6 text-primary" />
                )}
              </div>
              <div>
                <p className="text-sm font-medium">{placeholder}</p>
                <p className="text-xs text-muted-foreground mt-1">Arraste uma imagem aqui ou clique para selecionar</p>
                <p className="text-xs text-muted-foreground">PNG, JPG até 5MB</p>
              </div>
            </>
          )}
        </div>
      </CardContent>

      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        onChange={handleFileInputChange}
        className="hidden"
        disabled={isUploading}
      />
    </Card>
  )
}
