"use client";

import { useParams } from "next/navigation";
import Feedback from "@/features/feedback/components/feedback";

export default function FeedbackPreviewPage() {
  const { token } = useParams<{ token: string }>();

  return <Feedback token={token} preview />;
}