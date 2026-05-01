import { MaterialIcons } from "@expo/vector-icons";
import { router } from "expo-router";
import { useCallback, useState } from "react";
import {
  Modal,
  ScrollView,
  StyleSheet,
  TextInput,
  View,
  Alert,
  KeyboardAvoidingView,
  Platform,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { HapticPressable } from "@/components/HapticPressable";
import { Header } from "@/components/Header";
import { StyledText } from "@/components/StyledText";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { useReminders, type ReminderList } from "@/contexts/RemindersContext";
import { n } from "@/utils/scaling";

type LongPressAction = "rename" | "delete";

export default function ListsScreen() {
  const { invertColors } = useInvertColors();
  const { lists, tasks, addList, renameList, deleteList, moveListUp, moveListDown } = useReminders();
  const bg = invertColors ? "white" : "black";
  const textColor = invertColors ? "black" : "white";
  const dimColor = invertColors ? "#AAAAAA" : "#555555";

  const [actionList, setActionList] = useState<ReminderList | null>(null);
  const [showActionMenu, setShowActionMenu] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);
  const [newListTitle, setNewListTitle] = useState("");
  const [showRenameModal, setShowRenameModal] = useState(false);
  const [renameTitle, setRenameTitle] = useState("");

  const sorted = [...lists].sort((a, b) => a.order - b.order);

  const handleLongPress = useCallback((list: ReminderList) => {
    setActionList(list);
    setShowActionMenu(true);
  }, []);

  const handleAction = useCallback((action: LongPressAction) => {
    setShowActionMenu(false);
    if (!actionList) return;
    if (action === "rename") {
      setRenameTitle(actionList.title);
      setShowRenameModal(true);
    } else if (action === "delete") {
      Alert.alert(
        "Delete List",
        `Delete "${actionList.title}"? Tasks will be moved to your default list.`,
        [
          { text: "Cancel", style: "cancel" },
          { text: "DELETE", style: "destructive", onPress: () => deleteList(actionList.id) },
        ]
      );
    }
  }, [actionList, deleteList]);

  const handleAddList = useCallback(() => {
    const t = newListTitle.trim();
    if (!t) return;
    addList(t);
    setNewListTitle("");
    setShowAddModal(false);
  }, [newListTitle, addList]);

  const handleRename = useCallback(() => {
    const t = renameTitle.trim();
    if (!t || !actionList) return;
    renameList(actionList.id, t);
    setShowRenameModal(false);
  }, [renameTitle, actionList, renameList]);

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: bg }]} edges={["top"]}>
      <Header headerTitle="Lists" hideBackButton />

      <ScrollView overScrollMode="never" showsVerticalScrollIndicator={false} style={styles.scroll}>
        {sorted.map((list, idx) => (
          <HapticPressable
            key={list.id}
            onPress={() => router.push({ pathname: "/list/[id]", params: { id: list.id } })}
            onLongPress={() => handleLongPress(list)}
            delayLongPress={400}
            style={styles.listItem}
          >
            <StyledText style={styles.listTitle}>{list.title}</StyledText>
            <View style={styles.arrowGroup}>
              <HapticPressable onPress={() => moveListUp(list.id)} disabled={idx === 0}>
                <MaterialIcons
                  name="keyboard-arrow-up"
                  size={n(28)}
                  color={idx === 0 ? dimColor : textColor}
                />
              </HapticPressable>
              <HapticPressable onPress={() => moveListDown(list.id)} disabled={idx === sorted.length - 1}>
                <MaterialIcons
                  name="keyboard-arrow-down"
                  size={n(28)}
                  color={idx === sorted.length - 1 ? dimColor : textColor}
                />
              </HapticPressable>
            </View>
          </HapticPressable>
        ))}

        <HapticPressable
          onPress={() => { setNewListTitle(""); setShowAddModal(true); }}
          style={styles.addRow}
        >
          <MaterialIcons name="add-circle-outline" size={n(30)} color={textColor} />
          <StyledText style={styles.addText}>New List</StyledText>
        </HapticPressable>
      </ScrollView>

      {/* Long-press action menu */}
      <Modal visible={showActionMenu} animationType="none" transparent statusBarTranslucent>
        <HapticPressable style={styles.menuBackdrop} onPress={() => setShowActionMenu(false)}>
          <View style={[styles.menuCard, { backgroundColor: bg }]}>
            <StyledText style={styles.menuListName}>{actionList?.title}</StyledText>
            {(["rename", "delete"] as LongPressAction[]).map((action) => (
              <HapticPressable
                key={action}
                onPress={() => handleAction(action)}
                style={styles.menuItem}
              >
                <StyledText style={[styles.menuItemText, action === "delete" && styles.menuItemDelete]}>
                  {action.toUpperCase()}
                </StyledText>
              </HapticPressable>
            ))}
          </View>
        </HapticPressable>
      </Modal>

      {/* Add list modal */}
      <Modal visible={showAddModal} animationType="none" transparent={false} statusBarTranslucent>
        <KeyboardAvoidingView
          style={[styles.inputBackdrop, { backgroundColor: bg }]}
          behavior={Platform.OS === "android" ? "height" : "padding"}
        >
          <SafeAreaView style={{ flex: 1 }} edges={["top"]}>
            <Header headerTitle="New List" rightAction={{ icon: "check", onPress: handleAddList }} />
            <View style={styles.inputArea}>
              <TextInput
                autoFocus
                value={newListTitle}
                onChangeText={setNewListTitle}
                placeholder="List name"
                placeholderTextColor={dimColor}
                onSubmitEditing={handleAddList}
                style={[styles.modalInput, { color: textColor }]}
                allowFontScaling={false}
              />
            </View>
          </SafeAreaView>
        </KeyboardAvoidingView>
      </Modal>

      {/* Rename modal */}
      <Modal visible={showRenameModal} animationType="none" transparent={false} statusBarTranslucent>
        <KeyboardAvoidingView
          style={[styles.inputBackdrop, { backgroundColor: bg }]}
          behavior={Platform.OS === "android" ? "height" : "padding"}
        >
          <SafeAreaView style={{ flex: 1 }} edges={["top"]}>
            <Header headerTitle="Rename" rightAction={{ icon: "check", onPress: handleRename }} />
            <View style={styles.inputArea}>
              <TextInput
                autoFocus
                value={renameTitle}
                onChangeText={setRenameTitle}
                onSubmitEditing={handleRename}
                placeholder="List name"
                placeholderTextColor={dimColor}
                style={[styles.modalInput, { color: textColor }]}
                allowFontScaling={false}
              />
            </View>
          </SafeAreaView>
        </KeyboardAvoidingView>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  scroll: { flex: 1 },
  listItem: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: n(22),
    paddingVertical: n(16),
  },
  listTitle: {
    fontSize: n(30),
    flex: 1,
  },
  arrowGroup: {
    flexDirection: "row",
    gap: n(4),
  },
  addRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: n(22),
    paddingVertical: n(20),
    gap: n(12),
  },
  addText: {
    fontSize: n(24),
    opacity: 0.5,
  },
  menuBackdrop: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.5)",
    justifyContent: "center",
    alignItems: "center",
    paddingHorizontal: n(40),
  },
  menuCard: {
    width: "100%",
  },
  menuListName: {
    fontSize: n(20),
    opacity: 0.5,
    paddingHorizontal: n(22),
    paddingVertical: n(16),
  },
  menuItem: {
    paddingHorizontal: n(22),
    paddingVertical: n(20),
  },
  menuItemText: {
    fontSize: n(24),
  },
  menuItemDelete: {
    opacity: 0.4,
  },
  inputBackdrop: {
    flex: 1,
  },
  inputArea: {
    paddingHorizontal: n(22),
    paddingTop: n(24),
  },
  modalInput: {
    fontSize: n(30),
    fontFamily: "PublicSans-Regular",
    paddingBottom: n(8),
  },
});
