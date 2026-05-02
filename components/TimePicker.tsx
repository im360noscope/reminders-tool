import { MaterialIcons } from "@expo/vector-icons";
import { Modal, StyleSheet, TouchableOpacity, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { StyledText } from "@/components/StyledText";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { n } from "@/utils/scaling";

// ─── Digit validation ─────────────────────────────────────────────────────────
// Digits fill right-to-left into fixed slots: [H][H]:[M][M]
// 1 digit  →  _:_D   (minutes tens)
// 2 digits →  _:DD   (minutes)
// 3 digits →  H:DD   (hours single + minutes)
// 4 digits →  HH:DD  (hours double + minutes)

function isValidNextDigit(current: string, next: string): boolean {
  const proposed = current + next;
  const len = proposed.length;

  if (len === 1) {
    // First digit is minutes-tens: must be 0-5
    // BUT we also allow any digit 0-9 here because with 3 digits
    // this becomes the hour — validate based on final position.
    // Actually: this digit ends up as minutes-tens after a 3rd digit is added,
    // OR as hours-ones if only 3 digits total.
    // Simplest valid rule: any digit 0-9 is ok at position 1.
    return true;
  }

  if (len === 2) {
    // Two digits = minutes (tens and ones): must be 00-59
    const mins = parseInt(proposed, 10);
    return mins >= 0 && mins <= 59;
  }

  if (len === 3) {
    // H:MM — first digit is hour (1-9 since leading 0 not shown for single digit)
    // minutes are last 2
    const h = parseInt(proposed[0], 10);
    const m = parseInt(proposed.slice(1), 10);
    return h >= 1 && h <= 12 && m >= 0 && m <= 59;
  }

  if (len === 4) {
    // HH:MM
    const h = parseInt(proposed.slice(0, 2), 10);
    const m = parseInt(proposed.slice(2), 10);
    return h >= 1 && h <= 12 && m >= 0 && m <= 59;
  }

  return false;
}

// ─── Display builder ──────────────────────────────────────────────────────────
// digits="" → "  :  "
// digits="7" → "  : 7"   (7 in minutes-tens slot)
// digits="73"→ "  :73"
// digits="730"→"7:30"
// digits="1230"→"12:30"

function buildDisplay(digits: string): string {
  switch (digits.length) {
    case 0: return "  :  ";
    case 1: return `  : ${digits[0]}`;
    case 2: return `  :${digits}`;
    case 3: return `${digits[0]}:${digits.slice(1)}`;
    case 4: return `${digits.slice(0, 2)}:${digits.slice(2)}`;
    default: return "  :  ";
  }
}

// ─── Component ────────────────────────────────────────────────────────────────

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
  const canConfirm = digits.length === 3 || digits.length === 4;

  const handleDigit = (d: string) => {
    if (digits.length >= 4) return;
    if (isValidNextDigit(digits, d)) {
      onDigit(d);
    }
  };

  const numRows = [["1", "2", "3"], ["4", "5", "6"], ["7", "8", "9"]];

  return (
    <Modal visible={visible} animationType="none" transparent={false} statusBarTranslucent>
      <SafeAreaView style={[styles.container, { backgroundColor: bg }]}>

        {/* AM/PM + time display */}
        <View style={styles.topSection}>
          <TouchableOpacity
            onPress={() => onAmPm("AM")}
            style={styles.ampmBtn}
            activeOpacity={1}
          >
            <StyledText style={styles.ampmText}>AM</StyledText>
            {ampm === "AM" && <View style={[styles.ampmUnderline, { backgroundColor: textColor }]} />}
          </TouchableOpacity>

          <StyledText style={styles.timeDisplay}>
            {buildDisplay(digits)}
          </StyledText>

          <TouchableOpacity
            onPress={() => onAmPm("PM")}
            style={styles.ampmBtn}
            activeOpacity={1}
          >
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
                  onPress={() => handleDigit(d)}
                  style={styles.numBtn}
                  activeOpacity={0.6}
                >
                  <StyledText style={styles.numText}>{d}</StyledText>
                </TouchableOpacity>
              ))}
            </View>
          ))}

          {/* Bottom row */}
          <View style={styles.numRow}>
            {/* Left: SAVE when can confirm, × dismiss when no digits, empty otherwise */}
            {canConfirm ? (
              <TouchableOpacity onPress={onConfirm} style={styles.numBtn} activeOpacity={0.6}>
                <StyledText style={styles.saveText}>SAVE</StyledText>
              </TouchableOpacity>
            ) : !hasDigits ? (
              <TouchableOpacity onPress={onDismiss} style={styles.numBtn} activeOpacity={0.6}>
                <StyledText style={styles.dismissX}>✕</StyledText>
              </TouchableOpacity>
            ) : (
              <View style={styles.numBtn} />
            )}

            {/* 0 */}
            <TouchableOpacity
              onPress={() => handleDigit("0")}
              style={styles.numBtn}
              activeOpacity={0.6}
            >
              <StyledText style={styles.numText}>0</StyledText>
            </TouchableOpacity>

            {/* Right: backspace chevron when has digits, empty otherwise */}
            {hasDigits ? (
              <TouchableOpacity onPress={onBackspace} style={styles.numBtn} activeOpacity={0.6}>
                <MaterialIcons name="chevron-left" size={n(44)} color={textColor} />
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
    fontSize: n(72),
    fontWeight: "200",
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
  saveText: {
    fontSize: n(20),
    fontFamily: "PublicSans-Regular",
  },
  dismissX: {
    fontSize: n(24),
  },
});
