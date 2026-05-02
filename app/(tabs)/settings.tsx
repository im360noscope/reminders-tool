import { router } from "expo-router";
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

const AFTER_QUICK_ADD_LABELS: Record<string, string> = {
  "toast": "Add Next",
  "go-to-list": "Go to List",
};

export default function SettingsScreen() {
  const { invertColors, setInvertColors } = useInvertColors();
  const { lists, settings, updateSettings } = useReminders();
  const bg = invertColors ? "white" : "black";
  const [showListPicker, setShowListPicker] = useState(false);

  const defaultList = lists.find(l => l.id === settings.defaultListId);

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: bg }]} edges={["top"]}>
      <Header headerTitle="Settings" hideBackButton />

      <ScrollView overScrollMode="never" showsVerticalScrollIndicator={false}>

        {/* Invert Colors */}
        <View style={styles.row}>
          <ToggleSwitch
            label="Invert Colors"
            value={invertColors}
            onValueChange={setInvertColors}
          />
        </View>

        {/* Default List */}
        <HapticPressable onPress={() => setShowListPicker(true)} style={styles.row}>
          <StyledText style={styles.selectorLabel}>Default List</StyledText>
          <StyledText style={styles.selectorValue}>{defaultList?.title ?? "Inbox"}</StyledText>
        </HapticPressable>

        {/* After Quick Add */}
        <HapticPressable
          onPress={() => router.push("/settings/after-quick-add")}
          style={styles.row}
        >
          <StyledText style={styles.selectorLabel}>After Quick Add</StyledText>
          <StyledText style={styles.selectorValue}>
            {AFTER_QUICK_ADD_LABELS[settings.afterAddBehavior] ?? "Add Next"}
          </StyledText>
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
    flexDirection: "column",
    alignItems: "flex-start",
  },
  selectorLabel: {
    fontSize: n(20),
    paddingTop: n(7.5),
    lineHeight: n(20),
  },
  selectorValue: {
    fontSize: n(30),
    paddingBottom: n(10),
  },
});
