import { StyleSheet, View } from "react-native";
import { HapticPressable } from "@/components/HapticPressable";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { n } from "@/utils/scaling";

interface TaskCheckboxProps {
  checked: boolean;
  onToggle: () => void;
  size?: number;
}

export function TaskCheckbox({ checked, onToggle, size = 22 }: TaskCheckboxProps) {
  const { invertColors } = useInvertColors();
  const color = invertColors ? "black" : "white";
  const dim = n(size);

  return (
    <HapticPressable onPress={onToggle} style={styles.hitArea}>
      <View
        style={[
          styles.circle,
          {
            width: dim,
            height: dim,
            borderRadius: dim / 2,
            borderColor: color,
            backgroundColor: checked ? color : "transparent",
          },
        ]}
      />
    </HapticPressable>
  );
}

const styles = StyleSheet.create({
  hitArea: {
    paddingHorizontal: n(14),
    paddingTop: n(17),    // nudged down slightly to optically center on first text line
    paddingBottom: n(8),
    alignSelf: "flex-start",
  },
  circle: {
    borderWidth: n(1.5),
  },
});
