import { StyleSheet, View } from "react-native";
import { HapticPressable } from "@/components/HapticPressable";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { n } from "@/utils/scaling";

interface TaskCheckboxProps {
  checked: boolean;
  onToggle: () => void;
  size?: number;
  paddingTop?: number;
}

export function TaskCheckbox({ checked, onToggle, size = 22, paddingTop = 17 }: TaskCheckboxProps) {
  const { invertColors } = useInvertColors();
  const color = invertColors ? "black" : "white";
  const dim = n(size);

  return (
    <HapticPressable onPress={onToggle} style={[styles.hitArea, { paddingTop: n(paddingTop) }]}>
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
    paddingBottom: n(8),
    alignSelf: "flex-start",
  },
  circle: {
    borderWidth: n(1.5),
  },
});
