import { MaterialIcons } from "@expo/vector-icons";
import { Modal, ScrollView, StyleSheet, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { HapticPressable } from "@/components/HapticPressable";
import { StyledText } from "@/components/StyledText";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import type { ReminderList } from "@/contexts/RemindersContext";
import { n } from "@/utils/scaling";

interface ListPickerModalProps {
  visible: boolean;
  lists: ReminderList[];
  selectedId?: string;
  onSelect: (list: ReminderList) => void;
  onDismiss: () => void;
}

export function ListPickerModal({
  visible,
  lists,
  selectedId,
  onSelect,
  onDismiss,
}: ListPickerModalProps) {
  const { invertColors } = useInvertColors();
  const bg = invertColors ? "white" : "black";
  const textColor = invertColors ? "black" : "white";
  const dividerColor = invertColors ? "#DDDDDD" : "#1A1A1A";
  const sorted = [...lists].sort((a, b) => a.order - b.order);

  return (
    <Modal visible={visible} animationType="none" transparent={false} statusBarTranslucent>
      <SafeAreaView style={[styles.container, { backgroundColor: bg }]}>
        <View style={[styles.header, { borderBottomColor: dividerColor }]}>
          <View style={styles.headerLeft} />
          <StyledText style={styles.headerTitle}>List</StyledText>
          <HapticPressable onPress={onDismiss} style={styles.headerRight}>
            <MaterialIcons name="close" size={n(28)} color={textColor} />
          </HapticPressable>
        </View>
        <ScrollView
          overScrollMode="never"
          showsVerticalScrollIndicator={false}
        >
          {sorted.map((list) => {
            const isSelected = list.id === selectedId;
            return (
              <HapticPressable
                key={list.id}
                onPress={() => onSelect(list)}
                style={[styles.item, { borderBottomColor: dividerColor }]}
              >
                <StyledText style={styles.itemText}>{list.title}</StyledText>
                {isSelected && (
                  <MaterialIcons name="check" size={n(28)} color={textColor} />
                )}
              </HapticPressable>
            );
          })}
        </ScrollView>
      </SafeAreaView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: n(22),
    paddingVertical: n(14),
    borderBottomWidth: n(1),
  },
  headerLeft: {
    width: n(32),
  },
  headerTitle: {
    fontSize: n(20),
  },
  headerRight: {
    width: n(32),
    alignItems: "flex-end",
  },
  item: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: n(22),
    paddingVertical: n(20),
    borderBottomWidth: n(1),
  },
  itemText: {
    fontSize: n(30),
  },
});
