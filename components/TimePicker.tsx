import { MaterialIcons } from "@expo/vector-icons";
import { Modal, StyleSheet, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { HapticPressable } from "@/components/HapticPressable";
import { StyledText } from "@/components/StyledText";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { n } from "@/utils/scaling";

// Digits are stored as a 4-char string built left to right e.g. "0930"
// Display: first 2 digits = hours, last 2 = minutes → "09:30"

interface TimePickerProps {
  visible: boolean;
  digits: string;       // up to 4 digit string
  ampm: "AM" | "PM";
  onDigit: (d: string) => void;
  onBackspace: () => void;
  onAmPm: (v: "AM" | "PM") => void;
  onConfirm: () => void;
  onDismiss: () => void;
}

function formatDisplay(digits: string): string {
  const padded = digits.padStart(4, " ");
  const h = padded.slice(0, 2);
  const m = padded.slice(2, 4);
  return `${h}:${m}`;
}

export function TimePicker({
  visible,
  digits,
  ampm,
  onDigit,
  onBackspace,
  onAmPm,
  onConfirm,
  onDismiss,
}: TimePickerProps) {
  const { invertColors } = useInvertColors();
  const bg = invertColors ? "white" : "black";
  const textColor = invertColors ? "black" : "white";
  const hasDigits = digits.length > 0;

  const displayText = digits.length === 0
    ? "  :  "
    : formatDisplay(digits);

  return (
    <Modal visible={visible} animationType="none" transparent={false} statusBarTranslucent>
      <SafeAreaView style={[styles.container, { backgroundColor: bg }]}>
        {/* AM/PM + Time Display */}
        <View style={styles.topRow}>
          <HapticPressable onPress={() => onAmPm("AM")} style={styles.ampmBtn}>
            <StyledText style={[styles.ampm, ampm === "AM" && styles.ampmActive]}>
              AM
            </StyledText>
            {ampm === "AM" && <View style={[styles.ampmUnderline, { backgroundColor: textColor }]} />}
          </HapticPressable>

          <StyledText style={styles.timeDisplay}>{displayText}</StyledText>

          <HapticPressable onPress={() => onAmPm("PM")} style={styles.ampmBtn}>
            <StyledText style={[styles.ampm, ampm === "PM" && styles.ampmActive]}>
              PM
            </StyledText>
            {ampm === "PM" && <View style={[styles.ampmUnderline, { backgroundColor: textColor }]} />}
          </HapticPressable>
        </View>

        {/* Numpad */}
        <View style={styles.numpad}>
          {[["1", "2", "3"], ["4", "5", "6"], ["7", "8", "9"]].map((row, ri) => (
            <View key={`row-${ri}`} style={styles.numRow}>
              {row.map((d) => (
                <HapticPressable
                  key={d}
                  onPress={() => onDigit(d)}
                  style={styles.numBtn}
                  disabled={digits.length >= 4}
                >
                  <StyledText style={[styles.numText, digits.length >= 4 && { opacity: 0.3 }]}>
                    {d}
                  </StyledText>
                </HapticPressable>
              ))}
            </View>
          ))}

          {/* Bottom row */}
          <View style={styles.numRow}>
            {/* Left: clock confirm (only when has digits) or empty */}
            {hasDigits ? (
              <HapticPressable onPress={onConfirm} style={styles.numBtn}>
                <MaterialIcons name="alarm" size={n(40)} color={textColor} />
              </HapticPressable>
            ) : (
              <View style={styles.numBtn} />
            )}

            {/* 0 */}
            <HapticPressable
              onPress={() => onDigit("0")}
              style={styles.numBtn}
              disabled={digits.length >= 4}
            >
              <StyledText style={[styles.numText, digits.length >= 4 && { opacity: 0.3 }]}>
                0
              </StyledText>
            </HapticPressable>

            {/* Right: backspace (when has digits) or dismiss X */}
            {hasDigits ? (
              <HapticPressable onPress={onBackspace} style={styles.numBtn}>
                <MaterialIcons name="chevron-left" size={n(44)} color={textColor} />
              </HapticPressable>
            ) : (
              <HapticPressable onPress={onDismiss} style={styles.numBtn}>
                <MaterialIcons name="close" size={n(36)} color={textColor} />
              </HapticPressable>
            )}
          </View>
        </View>
      </SafeAreaView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingHorizontal: n(22),
    justifyContent: "space-between",
    paddingBottom: n(40),
  },
  topRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingTop: n(48),
    paddingHorizontal: n(8),
  },
  ampmBtn: {
    alignItems: "center",
    minWidth: n(60),
  },
  ampm: {
    fontSize: n(22),
    opacity: 0.4,
  },
  ampmActive: {
    opacity: 1,
  },
  ampmUnderline: {
    height: n(1.5),
    width: "100%",
    marginTop: n(3),
  },
  timeDisplay: {
    fontSize: n(80),
    letterSpacing: n(2),
    fontFamily: "PublicSans-Regular",
  },
  numpad: {
    paddingBottom: n(10),
  },
  numRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: n(8),
  },
  numBtn: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: n(20),
  },
  numText: {
    fontSize: n(40),
    fontFamily: "PublicSans-Regular",
  },
});
