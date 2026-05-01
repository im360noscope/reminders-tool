import { ScrollView, StyleSheet, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Header } from "@/components/Header";
import { HapticPressable } from "@/components/HapticPressable";
import { ListPickerModal } from "@/components/ListPickerModal";
import { StyledText } from "@/components/StyledText";
import { ToggleSwitch } from "@/components/ToggleSwitch";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { useReminders } from "@/contexts/RemindersContext";
import { n } from "@/utils/scaling";
import { useState } from "react";

export default function SettingsScreen() {
  const { invertColors, setInvertColors } = useInvertColors();
  const { lists, settings, updateSettings } = useReminders();
  const bg = invertColors ? "white" : "black";
  const [showListPicker, setShowListPicker] = useState(false);

  const defaultList = lists.find(l => l.id === settings.defaultListId);
  const isToast = settings.afterAddBehavior === "toast";

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: bg }]} edges={["top"]}>
      <Header headerTitle="Settings" hideBackButton />

      <ScrollView overScrollMode="never" showsVerticalScrollIndicator={false}>

        <View style={styles.row}>
          <ToggleSwitch
            label="Invert Colors"
            value={invertColors}
            onValueChange={setInvertColors}
          />
        </View>

        <HapticPressable onPress={() => setShowListPicker(true)} style={styles.row}>
          <View style={styles.rowInner}>
            <StyledText style={styles.rowLabel}>Default List</StyledText>
            <StyledText style={styles.rowValue}>{defaultList?.title ?? "Inbox"}</StyledText>
          </View>
        </HapticPressable>

        <View style={styles.row}>
          <StyledText style={styles.sectionLabel}>After Adding a Task</StyledText>
        </View>

        <HapticPressable
          onPress={() => updateSettings({ afterAddBehavior: "toast" })}
          style={styles.row}
        >
          <StyledText style={[styles.optionText, !isToast && styles.optionInactive]}>
            Show confirmation
          </StyledText>
          {isToast && <StyledText style={styles.checkmark}>✓</StyledText>}
        </HapticPressable>

        <HapticPressable
          onPress={() => updateSettings({ afterAddBehavior: "go-to-list" })}
          style={styles.row}
        >
          <StyledText style={[styles.optionText, isToast && styles.optionInactive]}>
            Go to list
          </StyledText>
          {!isToast && <StyledText style={styles.checkmark}>✓</StyledText>}
        </HapticPressable>

      </ScrollView>

      <ListPickerModal
        visible={showListPicker}
        lists={lists}
        selectedId={settings.defaultListId}
        onSelect={(list) => { updateSettings({ defaultListId: list.id }); setShowListPicker(false); }}
        onDismiss={() => setShowListPicker(false)}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  row: {
    paddingHorizontal: n(22),
    paddingVertical: n(16),
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  rowInner: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  rowLabel: { fontSize: n(30) },
  rowValue: { fontSize: n(22), opacity: 0.5 },
  sectionLabel: { fontSize: n(16), opacity: 0.4 },
  optionText: { fontSize: n(28) },
  optionInactive: { opacity: 0.3 },
  checkmark: { fontSize: n(22) },
});
