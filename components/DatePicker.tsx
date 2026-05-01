import { MaterialIcons } from "@expo/vector-icons";
import { useMemo } from "react";
import { Modal, StyleSheet, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { HapticPressable } from "@/components/HapticPressable";
import { StyledText } from "@/components/StyledText";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { n } from "@/utils/scaling";

const DAY_HEADERS = ["S", "M", "T", "W", "T", "F", "S"];

const MONTH_NAMES = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];

interface DatePickerProps {
  visible: boolean;
  value?: string; // "YYYY-MM-DD"
  onSelect: (date: string) => void;
  onDismiss: () => void;
  // Internal navigation state
  viewYear: number;
  viewMonth: number; // 0-indexed
  onPrevMonth: () => void;
  onNextMonth: () => void;
}

export function DatePicker({
  visible,
  value,
  onSelect,
  onDismiss,
  viewYear,
  viewMonth,
  onPrevMonth,
  onNextMonth,
}: DatePickerProps) {
  const { invertColors } = useInvertColors();
  const bg = invertColors ? "white" : "black";
  const textColor = invertColors ? "black" : "white";

  const today = new Date();
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;

  // Build calendar grid
  const { days } = useMemo(() => {
    const firstDay = new Date(viewYear, viewMonth, 1).getDay(); // 0=Sun
    const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();

    const cells: (number | null)[] = [];
    for (let i = 0; i < firstDay; i++) cells.push(null);
    for (let d = 1; d <= daysInMonth; d++) cells.push(d);

    return { days: cells };
  }, [viewYear, viewMonth]);

  const handleDayPress = (day: number) => {
    const dateStr = `${viewYear}-${String(viewMonth + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
    onSelect(dateStr);
  };

  const rows: (number | null)[][] = [];
  for (let i = 0; i < days.length; i += 7) {
    rows.push(days.slice(i, i + 7).concat(Array(7).fill(null)).slice(0, 7));
  }

  return (
    <Modal visible={visible} animationType="none" transparent={false} statusBarTranslucent>
      <SafeAreaView style={[styles.container, { backgroundColor: bg }]}>
        {/* Header */}
        <View style={styles.header}>
          <HapticPressable onPress={onPrevMonth} style={styles.navBtn}>
            <MaterialIcons name="chevron-left" size={n(36)} color={textColor} />
          </HapticPressable>
          <StyledText style={styles.monthTitle}>
            {MONTH_NAMES[viewMonth]} {viewYear}
          </StyledText>
          <HapticPressable onPress={onNextMonth} style={styles.navBtn}>
            <MaterialIcons name="chevron-right" size={n(36)} color={textColor} />
          </HapticPressable>
        </View>

        {/* Day headers */}
        <View style={styles.dayHeaderRow}>
          {DAY_HEADERS.map((d, i) => (
            <View key={`h-${i}`} style={styles.cell}>
              <StyledText style={styles.dayHeader}>{d}</StyledText>
            </View>
          ))}
        </View>

        {/* Calendar grid */}
        <View style={styles.grid}>
          {rows.map((row, ri) => (
            <View key={`row-${ri}`} style={styles.row}>
              {row.map((day, ci) => {
                if (!day) {
                  return <View key={`empty-${ci}`} style={styles.cell} />;
                }
                const dateStr = `${viewYear}-${String(viewMonth + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
                const isToday = dateStr === todayStr;
                const isSelected = dateStr === value;

                return (
                  <HapticPressable
                    key={`day-${day}`}
                    onPress={() => handleDayPress(day)}
                    style={styles.cell}
                  >
                    <StyledText
                      style={[
                        styles.dayText,
                        isSelected && { fontFamily: "PublicSans-Regular", opacity: 0.5 },
                        isToday && styles.todayText,
                      ]}
                    >
                      {day}
                    </StyledText>
                    {isToday && <View style={[styles.todayUnderline, { backgroundColor: textColor }]} />}
                  </HapticPressable>
                );
              })}
            </View>
          ))}
        </View>

        {/* Dismiss */}
        <View style={styles.footer}>
          <HapticPressable onPress={onDismiss}>
            <MaterialIcons name="close" size={n(36)} color={textColor} />
          </HapticPressable>
        </View>
      </SafeAreaView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingHorizontal: n(22),
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingVertical: n(18),
  },
  navBtn: {
    padding: n(4),
  },
  monthTitle: {
    fontSize: n(22),
  },
  dayHeaderRow: {
    flexDirection: "row",
    marginBottom: n(8),
  },
  dayHeader: {
    fontSize: n(18),
    textAlign: "center",
    opacity: 0.6,
  },
  grid: {
    flex: 1,
    justifyContent: "flex-start",
  },
  row: {
    flexDirection: "row",
    marginBottom: n(4),
  },
  cell: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: n(10),
    position: "relative",
  },
  dayText: {
    fontSize: n(26),
    textAlign: "center",
  },
  todayText: {
    // no extra style needed — underline handles it
  },
  todayUnderline: {
    position: "absolute",
    bottom: n(6),
    width: n(16),
    height: n(1.5),
  },
  footer: {
    alignItems: "center",
    paddingVertical: n(28),
  },
});
