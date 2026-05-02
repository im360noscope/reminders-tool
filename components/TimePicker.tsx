import { MaterialIcons } from "@expo/vector-icons";
import { Modal, StyleSheet, TouchableOpacity, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { StyledText } from "@/components/StyledText";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { n } from "@/utils/scaling";

function formatDisplay(digits: string): string {
  // Build display like LightOS: digits fill in from left
  // 0 digits: "  :  ", 1: " _:  ", etc.
  const padded = digits.padEnd(4, " ");
  const h = padded.slice(0, 2).trimStart() || " ";
  const m = padded.slice(2, 4);
  // Show as "7:30" style — trim leading zero from hour
  const hDisplay = digits.length === 0 ? "  " : digits.length === 1 ? digits[0] : String(parseInt(digits.slice(0, 2), 10));
  const mDisplay = digits.length <= 2 ? (digits.length === 0 ? "  " : "  ") : padded.slice(2, 4);
  return `${hDisplay}:${mDisplay}`;
}

interface TimePickerProps {
  visible: boolean;
  digits: string;
  ampm: "AM" | "PM";
  onDigit: (d: string) => void;
  onBackspace: () => void;
  onAmPm: (v: "AM" | "PM") => void;
  onConfirm: () => void;
  onDismiss: () => void;
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

  // Build display time string
  let timeDisplay = "";
  if (digits.length === 0) {
    timeDisplay = " : ";
  } else {
    const hRaw = digits.slice(0, Math.min(2, digits.length));
    const mRaw = digits.slice(2, 4);
    const hNum = parseInt(hRaw, 10);
    const hStr = digits.length >= 2 ? String(hNum) : hRaw;
    const mStr = digits.length >= 3 ? mRaw.padEnd(2, " ") : digits.length === 2 ? "  " : "  ";
    timeDisplay = `${hStr}:${mStr.length > 0 ? mRaw : "  "}`;
  }

  // Simpler, reliable display builder
  const buildDisplay = () => {
    if (digits.length === 0) return " : ";
    const h = digits.slice(0, Math.min(2, digits.length));
    const m = digits.slice(2, 4);
    const hNum = parseInt(h.padEnd(2, "0"), 10);
    const hDisplay = digits.length >= 2 ? String(hNum) : h;
    const mDisplay = digits.length >= 3 ? m.padEnd(2, " ") : "";
    return `${hDisplay}:${mDisplay}`;
  };

  const numRows = [["1", "2", "3"], ["4", "5", "6"], ["7", "8", "9"]];

  return (
    <Modal visible={visible} animationType="none" transparent={false} statusBarTranslucent>
      <SafeAreaView style={[styles.container, { backgroundColor: bg }]}>

        {/* Time display + AM/PM */}
        <View style={styles.topSection}>
          <TouchableOpacity onPress={() => onAmPm("AM")} style={styles.ampmBtn} activeOpacity={0.7}>
            <StyledText style={styles.ampmText}>AM</StyledText>
            {ampm === "AM" && <View style={[styles.ampmUnderline, { backgroundColor: textColor }]} />}
          </TouchableOpacity>

          <StyledText style={styles.timeDisplay} numberOfLines={1} adjustsFontSizeToFit={false}>
            {buildDisplay()}
          </StyledText>

          <TouchableOpacity onPress={() => onAmPm("PM")} style={styles.ampmBtn} activeOpacity={0.7}>
            <StyledText style={styles.ampmText}>PM</StyledText>
            {ampm === "PM" && <View style={[styles.ampmUnderline, { backgroundColor: textColor }]} />}
          </TouchableOpacity>
        </View>

        {/* Numpad */}
        <View style={styles.numpad}>
          {numRows.map((row, ri) => (
            <View key={`row-${ri}`} style={styles.numRow}>
              {row.map((d) => (
                <TouchableOpacity
                  key={d}
                  onPress={() => onDigit(d)}
                  style={styles.numBtn}
                  activeOpacity={0.6}
                  disabled={digits.length >= 4}
                >
                  <StyledText style={[styles.numText, digits.length >= 4 && styles.numDisabled]}>
                    {d}
                  </StyledText>
                </TouchableOpacity>
              ))}
            </View>
          ))}

          {/* Bottom row: SAVE | 0 | < */}
          <View style={styles.numRow}>
            {hasDigits ? (
              <TouchableOpacity onPress={onConfirm} style={styles.numBtn} activeOpacity={0.6}>
                <StyledText style={styles.saveText}>SAVE</StyledText>
              </TouchableOpacity>
            ) : (
              <TouchableOpacity onPress={onDismiss} style={styles.numBtn} activeOpacity={0.6}>
                <StyledText style={styles.dismissX}>✕</StyledText>
              </TouchableOpacity>
            )}

            <TouchableOpacity
              onPress={() => onDigit("0")}
              style={styles.numBtn}
              activeOpacity={0.6}
              disabled={digits.length >= 4}
            >
              <StyledText style={[styles.numText, digits.length >= 4 && styles.numDisabled]}>
                0
              </StyledText>
            </TouchableOpacity>

            {hasDigits ? (
              <TouchableOpacity onPress={onBackspace} style={styles.numBtn} activeOpacity={0.6}>
                <StyledText style={styles.backText}>{"<"}</StyledText>
              </TouchableOpacity>
            ) : (
              <View style={styles.numBtn} />
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
    paddingHorizontal: n(16),
  },
  topSection: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingTop: n(24),
    paddingBottom: n(24),
    paddingHorizontal: n(8),
  },
  ampmBtn: {
    alignItems: "center",
    minWidth: n(52),
  },
  ampmText: {
    fontSize: n(20),
  },
  ampmUnderline: {
    height: n(1.5),
    width: "100%",
    marginTop: n(3),
  },
  timeDisplay: {
    fontSize: n(80),
    fontFamily: "PublicSans-Regular",
    textAlign: "center",
    includeFontPadding: false,
  },
  numpad: {
    flex: 1,
    justifyContent: "space-evenly",
    paddingBottom: n(16),
  },
  numRow: {
    flexDirection: "row",
    justifyContent: "space-between",
  },
  numBtn: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: n(12),
  },
  numText: {
    fontSize: n(36),
    fontFamily: "PublicSans-Regular",
  },
  numDisabled: {
    opacity: 0.2,
  },
  saveText: {
    fontSize: n(20),
    fontFamily: "PublicSans-Regular",
  },
  backText: {
    fontSize: n(30),
    fontFamily: "PublicSans-Regular",
  },
  dismissX: {
    fontSize: n(24),
  },
});
