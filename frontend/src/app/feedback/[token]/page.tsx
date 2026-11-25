import Feedback from "@/features/feedback/components/feedback";
export default async function Page({ params }: { params: Promise<{ token: string }> }) {
  return <Feedback token={(await params).token} />;
}
