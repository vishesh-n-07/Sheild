
import { createClient } from '@supabase/supabase-js'

const supabaseUrl = "https://ihnqpatlwecnyvvkknqi.supabase.co"
const supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlobnFwYXRsd2Vjbnl2dmtrbnFpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzcwMzE3ODMsImV4cCI6MjA5MjYwNzc4M30.wQxw31r5BTJL2vLNt2u3aYih2WpGd1DBfhvO6pxyDvc"

export const supabase = createClient(supabaseUrl, supabaseKey)



