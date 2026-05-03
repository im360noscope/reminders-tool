import { useInvertColors } from "@/contexts/InvertColorsContext";
import { TaskForm } from "@/components/TaskForm";

export default function AddScreen() {
  const { invertColors } = useInvertColors();
  return (
    <TaskForm
      onSaved={() => {
        // ADD tab stays on screen after saving — form resets itself
      }}
    />
  );
}
