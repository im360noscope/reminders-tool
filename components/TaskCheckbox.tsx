import { StyleSheet, View } from "react-native";
import { HapticPressable } from "@/components/HapticPressable";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { n } from "@/utils/scaling";

interface TaskCheckboxProps {
  checked: boolean;
  onToggle: () => void;
  size?: number;
}

export function TaskCheckbox({ checked, onToggle, size = 24 }: TaskCheckboxProps) {
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
    padding: n(8),
    alignItems: "center",
    justifyContent: "center",
  },
  circle: {
    borderWidth: n(1.5),
  },
});
