import { useCallback, useState } from "react";
import {
  Keyboard,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  TextInput as RNTextInput,
  TouchableWithoutFeedback,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Toast } from "@/components/Toast";
import { DatePicker } from "@/components/DatePicker";
import { Header } from "@/components/Header";
import { HapticPressable } from "@/components/HapticPressable";
import { ListPickerModal } from "@/components/ListPickerModal";
import { StyledText } from "@/components/StyledText";
import { TimePicker } from "@/components/TimePicker";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { useReminders } from "@/contexts/RemindersContext";
import { n } from "@/utils/scaling";
import { router } from "expo-router";

function formatDisplayDate(dateStr: string): string {
  const [y, mo, d] = dateStr.split("-").map(Number);
  const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  return `${months[mo - 1]} ${d}, ${y}`;
}

function formatDisplayTime(digits: string, ampm: "AM" | "PM"): string {
  const h = parseInt(digits.slice(0, 2), 10);
  const m = digits.slice(2, 4);
  return `${h}:${m} ${ampm}`;
}

function digitsToTime(digits: string, ampm: "AM" | "PM"): string {
  let h = parseInt(digits.slice(0, 2), 10);
  const m = digits.slice(2, 4);
  if (ampm === "PM" && h !== 12) h += 12;
  if (ampm === "AM" && h === 12) h = 0;
  return `${String(h).padStart(2, "0")}:${m}`;
}

export default function AddScreen() {
  const { invertColors } = useInvertColors();
  const { lists, settings, addTask } = useReminders();
  const bg = invertColors ? "white" : "black";
  const textColor = invertColors ? "black" : "white";
  const dimColor = invertColors ? "#AAAAAA" : "#555555";

  const [title, setTitle] = useState("");
  const [selectedListId, setSelectedListId] = useState<string>(settings.defaultListId);
  const [date, setDate] = useState<string | undefined>();
  const [showDatePicker, setShowDatePicker] = useState(false);
  const now = new Date();
  const [viewYear, setViewYear] = useState(now.getFullYear());
  const [viewMonth, setViewMonth] = useState(now.getMonth());
  const [timeDigits, setTimeDigits] = useState("");
  const [ampm, setAmPm] = useState<"AM" | "PM">("AM");
  const [showTimePicker, setShowTimePicker] = useState(false);
  const [confirmedTime, setConfirmedTime] = useState<string | undefined>();
  const [showListPicker, setShowListPicker] = useState(false);
  const [toastVisible, setToastVisible] = useState(false);
  

  const selectedList = lists.find(l => l.id === selectedListId) ?? lists[0];
  const canSave = title.trim().length > 0;

  const handleSave = useCallback(() => {
    if (!canSave) return;
    const task = addTask({
      title: title.trim(),
      listId: selectedListId,
      date,
      time: confirmedTime,
    });
    
    if (settings.afterAddBehavior === "toast") {
      
      setToastVisible(true);
    } else {
      router.push({ pathname: "/list/[id]", params: { id: selectedListId } });
    }
    setTitle("");
    setDate(undefined);
    setConfirmedTime(undefined);
    setTimeDigits("");
    setAmPm("AM");
  }, [canSave, title, selectedListId, date, confirmedTime, lists, settings, addTask]);

  const handleTimeConfirm = useCallback(() => {
    if (timeDigits.length !== 4) return;
    setConfirmedTime(digitsToTime(timeDigits, ampm));
    setShowTimePicker(false);
  }, [timeDigits, ampm]);

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: bg }]} edges={["top"]}>
      <Header
        headerTitle="Add"
        hideBackButton
        rightAction={{ icon: "check", onPress: handleSave, show: canSave }}
      />

      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === "android" ? "height" : "padding"}
      >
        <TouchableWithoutFeedback onPress={Keyboard.dismiss}>
        <ScrollView
          overScrollMode="never"
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
          style={styles.scroll}
        >
          {/* Task name */}
          <View style={styles.field}>
            <RNTextInput
              value={title}
              onChangeText={setTitle}
              placeholder="Task name"
              placeholderTextColor={dimColor}
              style={[styles.titleInput, { color: textColor }]}
              allowFontScaling={false}
              autoFocus
              returnKeyType="done"
              onSubmitEditing={Keyboard.dismiss}
            />
          </View>

          {/* List */}
          <HapticPressable onPress={() => setShowListPicker(true)} style={styles.field}>
            <StyledText style={styles.fieldLabel}>List</StyledText>
            <StyledText style={styles.fieldValue}>{selectedList?.title ?? "Inbox"}</StyledText>
          </HapticPressable>

          {/* Date */}
          <HapticPressable onPress={() => setShowDatePicker(true)} style={styles.field}>
            <StyledText style={styles.fieldLabel}>Date</StyledText>
            {date ? (
              <View style={styles.fieldValueRow}>
                <StyledText style={styles.fieldValue}>{formatDisplayDate(date)}</StyledText>
                <HapticPressable onPress={() => { setDate(undefined); setConfirmedTime(undefined); setTimeDigits(""); setAmPm("AM"); }}>
                  <StyledText style={styles.clearBtn}>CLEAR</StyledText>
                </HapticPressable>
              </View>
            ) : (
              <StyledText style={[styles.fieldValue, { color: dimColor }]}>None</StyledText>
            )}
          </HapticPressable>

          {/* Time */}
          {date && (
            <HapticPressable onPress={() => setShowTimePicker(true)} style={styles.field}>
              <StyledText style={styles.fieldLabel}>Time</StyledText>
              {confirmedTime ? (
                <View style={styles.fieldValueRow}>
                  <StyledText style={styles.fieldValue}>
                    {formatDisplayTime(timeDigits, ampm)}
                  </StyledText>
                  <HapticPressable onPress={() => { setConfirmedTime(undefined); setTimeDigits(""); setAmPm("AM"); }}>
                    <StyledText style={styles.clearBtn}>CLEAR</StyledText>
                  </HapticPressable>
                </View>
              ) : (
                <StyledText style={[styles.fieldValue, { color: dimColor }]}>None</StyledText>
              )}
            </HapticPressable>
          )}
        </ScrollView>
        </TouchableWithoutFeedback>
      </KeyboardAvoidingView>

      <DatePicker
        visible={showDatePicker}
        value={date}
        onSelect={(d) => { setDate(d); setShowDatePicker(false); }}
        onDismiss={() => setShowDatePicker(false)}
        viewYear={viewYear}
        viewMonth={viewMonth}
        onPrevMonth={() => {
          if (viewMonth === 0) { setViewMonth(11); setViewYear(y => y - 1); }
          else setViewMonth(m => m - 1);
        }}
        onNextMonth={() => {
          if (viewMonth === 11) { setViewMonth(0); setViewYear(y => y + 1); }
          else setViewMonth(m => m + 1);
        }}
      />

      <TimePicker
        visible={showTimePicker}
        digits={timeDigits}
        ampm={ampm}
        onDigit={(d) => setTimeDigits(prev => prev.length < 4 ? prev + d : prev)}
        onBackspace={() => setTimeDigits(prev => prev.slice(0, -1))}
        onAmPm={setAmPm}
        onConfirm={handleTimeConfirm}
        onDismiss={() => setShowTimePicker(false)}
      />

      <ListPickerModal
        visible={showListPicker}
        lists={lists}
        selectedId={selectedListId}
        onSelect={(list) => { setSelectedListId(list.id); setShowListPicker(false); }}
        onDismiss={() => setShowListPicker(false)}
      />

      <Toast
          message="added"
        visible={toastVisible}
        onHide={() => setToastVisible(false)}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  scroll: { flex: 1 },
  field: {
    paddingHorizontal: n(22),
    paddingVertical: n(18),
  },
  fieldLabel: {
    fontSize: n(14),
    opacity: 0.4,
    marginBottom: n(4),
  },
  fieldValue: {
    fontSize: n(24),
    fontFamily: "PublicSans-Regular",
  },
  fieldValueRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  clearBtn: {
    fontSize: n(14),
    opacity: 0.4,
  },
  titleInput: {
    fontSize: n(30),
    fontFamily: "PublicSans-Regular",
    paddingVertical: n(4),
  },
});
