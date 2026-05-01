import { useCallback, useRef, useState } from "react";
import {
  Keyboard,
  ScrollView,
  StyleSheet,
  TextInput as RNTextInput,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { AddToast } from "@/components/AddToast";
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
  const h = digits.slice(0, 2);
  const m = digits.slice(2, 4);
  return `${parseInt(h, 10)}:${m} ${ampm}`;
}

function digitsToTime(digits: string, ampm: "AM" | "PM"): string {
  // Returns HH:MM in 24h format for storage
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
  const dividerColor = invertColors ? "#DDDDDD" : "#1A1A1A";
  const placeholderColor = invertColors ? "#AAAAAA" : "#555555";

  const inputRef = useRef<RNTextInput>(null);

  // Form state
  const [title, setTitle] = useState("");
  const [selectedListId, setSelectedListId] = useState<string>(settings.defaultListId);

  // Date
  const [date, setDate] = useState<string | undefined>();
  const [showDatePicker, setShowDatePicker] = useState(false);
  const now = new Date();
  const [viewYear, setViewYear] = useState(now.getFullYear());
  const [viewMonth, setViewMonth] = useState(now.getMonth());

  // Time
  const [timeDigits, setTimeDigits] = useState("");
  const [ampm, setAmPm] = useState<"AM" | "PM">("AM");
  const [showTimePicker, setShowTimePicker] = useState(false);
  const [confirmedTime, setConfirmedTime] = useState<string | undefined>(); // HH:MM 24h

  // List picker
  const [showListPicker, setShowListPicker] = useState(false);

  // Toast
  const [toastVisible, setToastVisible] = useState(false);
  const [toastTask, setToastTask] = useState({ title: "", listTitle: "" });

  const selectedList = lists.find(l => l.id === selectedListId) ?? lists[0];
  const canSave = title.trim().length > 0;

  const handleSave = useCallback(() => {
    if (!canSave) return;
    Keyboard.dismiss();

    const task = addTask({
      title: title.trim(),
      listId: selectedListId,
      date,
      time: confirmedTime,
    });

    const listTitle = lists.find(l => l.id === selectedListId)?.title ?? "";

    if (settings.afterAddBehavior === "toast") {
      setToastTask({ title: task.title, listTitle });
      setToastVisible(true);
    } else {
      router.push({ pathname: "/list/[id]", params: { id: selectedListId } });
    }

    // Reset form (keep last list)
    setTitle("");
    setDate(undefined);
    setConfirmedTime(undefined);
    setTimeDigits("");
    setAmPm("AM");
  }, [canSave, title, selectedListId, date, confirmedTime, lists, settings, addTask]);

  const handleDateSelect = useCallback((d: string) => {
    setDate(d);
    setShowDatePicker(false);
  }, []);

  const handleTimeConfirm = useCallback(() => {
    if (timeDigits.length !== 4) return;
    const t24 = digitsToTime(timeDigits, ampm);
    setConfirmedTime(t24);
    setShowTimePicker(false);
  }, [timeDigits, ampm]);

  const handleDigit = useCallback((d: string) => {
    setTimeDigits(prev => prev.length < 4 ? prev + d : prev);
  }, []);

  const handleBackspace = useCallback(() => {
    setTimeDigits(prev => prev.slice(0, -1));
  }, []);

  const handleClearDate = useCallback(() => {
    setDate(undefined);
    setConfirmedTime(undefined);
    setTimeDigits("");
    setAmPm("AM");
  }, []);

  const handleClearTime = useCallback(() => {
    setConfirmedTime(undefined);
    setTimeDigits("");
    setAmPm("AM");
  }, []);

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: bg }]} edges={["top"]}>
      <Header
        headerTitle="Add"
        hideBackButton
        rightAction={{ icon: "check", onPress: handleSave, show: canSave }}
      />

      <ScrollView
        overScrollMode="never"
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
        style={styles.scroll}
      >
        {/* Task name */}
        <View style={[styles.field, { borderBottomColor: dividerColor }]}>
          <RNTextInput
            ref={inputRef}
            value={title}
            onChangeText={setTitle}
            placeholder="Task name"
            placeholderTextColor={placeholderColor}
            style={[styles.titleInput, { color: textColor }]}
            allowFontScaling={false}
            autoFocus
            returnKeyType="done"
            onSubmitEditing={handleSave}
          />
        </View>

        {/* List */}
        <HapticPressable
          onPress={() => setShowListPicker(true)}
          style={[styles.field, { borderBottomColor: dividerColor }]}
        >
          <StyledText style={styles.fieldLabel}>List</StyledText>
          <StyledText style={[styles.fieldValue, { color: textColor }]}>
            {selectedList?.title ?? "Inbox"}
          </StyledText>
        </HapticPressable>

        {/* Date */}
        <HapticPressable
          onPress={() => setShowDatePicker(true)}
          style={[styles.field, { borderBottomColor: dividerColor }]}
        >
          <StyledText style={styles.fieldLabel}>Date</StyledText>
          {date ? (
            <View style={styles.fieldValueRow}>
              <StyledText style={[styles.fieldValue, { color: textColor }]}>
                {formatDisplayDate(date)}
              </StyledText>
              <HapticPressable onPress={handleClearDate}>
                <StyledText style={styles.clearBtn}>CLEAR</StyledText>
              </HapticPressable>
            </View>
          ) : (
            <StyledText style={[styles.fieldValue, { color: placeholderColor }]}>
              None
            </StyledText>
          )}
        </HapticPressable>

        {/* Time — only available if date is set */}
        {date && (
          <HapticPressable
            onPress={() => setShowTimePicker(true)}
            style={[styles.field, { borderBottomColor: dividerColor }]}
          >
            <StyledText style={styles.fieldLabel}>Time</StyledText>
            {confirmedTime ? (
              <View style={styles.fieldValueRow}>
                <StyledText style={[styles.fieldValue, { color: textColor }]}>
                  {formatDisplayTime(
                    timeDigits.length === 4 ? timeDigits : confirmedTime.replace(":", ""),
                    ampm
                  )}
                </StyledText>
                <HapticPressable onPress={handleClearTime}>
                  <StyledText style={styles.clearBtn}>CLEAR</StyledText>
                </HapticPressable>
              </View>
            ) : (
              <StyledText style={[styles.fieldValue, { color: placeholderColor }]}>
                None
              </StyledText>
            )}
          </HapticPressable>
        )}
      </ScrollView>

      {/* Pickers */}
      <DatePicker
        visible={showDatePicker}
        value={date}
        onSelect={handleDateSelect}
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
        onDigit={handleDigit}
        onBackspace={handleBackspace}
        onAmPm={setAmPm}
        onConfirm={handleTimeConfirm}
        onDismiss={() => setShowTimePicker(false)}
      />

      <ListPickerModal
        visible={showListPicker}
        lists={lists}
        selectedId={selectedListId}
        onSelect={(list) => {
          setSelectedListId(list.id);
          setShowListPicker(false);
        }}
        onDismiss={() => setShowListPicker(false)}
      />

      {/* Full-screen toast */}
      <AddToast
        visible={toastVisible}
        taskTitle={toastTask.title}
        listTitle={toastTask.listTitle}
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
    borderBottomWidth: n(1),
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
