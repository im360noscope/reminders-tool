import { useEffect } from "react";
import { Modal, StyleSheet, View } from "react-native";
import { StyledText } from "@/components/StyledText";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { n } from "@/utils/scaling";

interface AddToastProps {
  visible: boolean;
  onHide: () => void;
}

export function AddToast({ visible, onHide }: AddToastProps) {
  const { invertColors } = useInvertColors();

  useEffect(() => {
    if (!visible) return;
    const timer = setTimeout(onHide, 1000);
    return () => clearTimeout(timer);
  }, [visible, onHide]);

  return (
    <Modal
      visible={visible}
      animationType="none"
      transparent={false}
      statusBarTranslucent
    >
      <View style={[styles.container, { backgroundColor: invertColors ? "white" : "black" }]}>
        <StyledText style={[styles.text, { color: invertColors ? "black" : "white" }]}>
          added
        </StyledText>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
  },
  text: {
    fontSize: n(40),
  },
});
