import { useEffect } from "react";
import { StyleSheet, View } from "react-native";
import { StyledText } from "@/components/StyledText";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { n } from "@/utils/scaling";

interface AddToastProps {
  visible: boolean;
  taskTitle: string;
  listTitle: string;
  onHide: () => void;
}

export function AddToast({ visible, taskTitle, listTitle, onHide }: AddToastProps) {
  const { invertColors } = useInvertColors();

  useEffect(() => {
    if (!visible) return;
    const timer = setTimeout(onHide, 1000);
    return () => clearTimeout(timer);
  }, [visible, onHide]);

  if (!visible) return null;

  return (
    <View
      style={[
        styles.overlay,
        { backgroundColor: invertColors ? "white" : "black" },
      ]}
    >
      <View style={styles.content}>
        <StyledText style={styles.label}>reminder added</StyledText>
        <StyledText style={styles.title}>{taskTitle}</StyledText>
        <StyledText style={styles.meta}>{listTitle}</StyledText>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    ...StyleSheet.absoluteFillObject,
    zIndex: 999,
    alignItems: "center",
    justifyContent: "center",
  },
  content: {
    alignItems: "center",
    paddingHorizontal: n(32),
    gap: n(12),
  },
  label: {
    fontSize: n(16),
    opacity: 0.5,
  },
  title: {
    fontSize: n(30),
    textAlign: "center",
  },
  meta: {
    fontSize: n(18),
    opacity: 0.5,
  },
});
