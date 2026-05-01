import { MaterialIcons } from "@expo/vector-icons";
import { router } from "expo-router";
import { useCallback, useState } from "react";
import {
  Alert,
  Modal,
  ScrollView,
  StyleSheet,
  TextInput,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { HapticPressable } from "@/components/HapticPressable";
import { Header } from "@/components/Header";
import { StyledText } from "@/components/StyledText";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { useReminders, type ReminderList } from "@/contexts/RemindersContext";
import { n } from "@/utils/scaling";

type LongPressAction = "rename" | "reorder" | "delete";

export default function ListsScreen() {
  const { invertColors } = useInvertColors();
  const { lists, tasks, addList, renameList, deleteList, moveListUp, moveListDown } = useReminders();
  const bg = invertColors ? "white" : "black";
  const textColor = invertColors ? "black" : "white";
  const dividerColor = invertColors ? "#DDDDDD" : "#1A1A1A";

  const [actionList, setActionList] = useState<ReminderList | null>(null);
  const [showActionMenu, setShowActionMenu] = useState(false);

  // Add list modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [newListTitle, setNewListTitle] = useState("");

  // Rename modal
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
    } else if (action === "reorder") {
      // Reorder is handled inline — just close the menu, user taps arrows
      // We surface a quick note (no modal needed — arrows are always visible)
    } else if (action === "delete") {
      Alert.alert(
        "Delete List",
        `Delete "${actionList.title}"? Tasks will be moved to your default list.`,
        [
          { text: "Cancel", style: "cancel" },
          {
            text: "DELETE",
            style: "destructive",
            onPress: () => deleteList(actionList.id),
          },
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

  const getTaskCount = (listId: string) =>
    tasks.filter(t => t.listId === listId && !t.completed).length;

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: bg }]} edges={["top"]}>
      <Header headerTitle="Lists" hideBackButton />

      <ScrollView
        overScrollMode="never"
        showsVerticalScrollIndicator={false}
        style={styles.scroll}
      >
        {sorted.map((list, idx) => (
          <HapticPressable
            key={list.id}
            onPress={() => router.push({ pathname: "/list/[id]", params: { id: list.id } })}
            onLongPress={() => handleLongPress(list)}
            delayLongPress={400}
            style={[styles.listItem, { borderBottomColor: dividerColor }]}
          >
            <View style={styles.listItemLeft}>
              <StyledText style={styles.listTitle}>{list.title}</StyledText>
            </View>
            <View style={styles.listItemRight}>
              <View style={styles.arrowGroup}>
                <HapticPressable
                  onPress={() => moveListUp(list.id)}
                  disabled={idx === 0}
                >
                  <MaterialIcons
                    name="keyboard-arrow-up"
                    size={n(28)}
                    color={idx === 0 ? dividerColor : textColor}
                  />
                </HapticPressable>
                <HapticPressable
                  onPress={() => moveListDown(list.id)}
                  disabled={idx === sorted.length - 1}
                >
                  <MaterialIcons
                    name="keyboard-arrow-down"
                    size={n(28)}
                    color={idx === sorted.length - 1 ? dividerColor : textColor}
                  />
                </HapticPressable>
              </View>
            </View>
          </HapticPressable>
        ))}

        {/* Add new list row */}
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
          <View style={[styles.menuCard, { backgroundColor: bg, borderColor: dividerColor }]}>
            <StyledText style={styles.menuListName}>{actionList?.title}</StyledText>
            {(["rename", "reorder", "delete"] as LongPressAction[]).map((action) => (
              <HapticPressable
                key={action}
                onPress={() => handleAction(action)}
                style={[styles.menuItem, { borderTopColor: dividerColor }]}
              >
                <StyledText
                  style={[
                    styles.menuItemText,
                    action === "delete" && styles.menuItemDelete,
                  ]}
                >
                  {action.toUpperCase()}
                </StyledText>
              </HapticPressable>
            ))}
          </View>
        </HapticPressable>
      </Modal>

      {/* Add list modal */}
      <Modal visible={showAddModal} animationType="none" transparent statusBarTranslucent>
        <View style={[styles.inputBackdrop, { backgroundColor: bg }]}>
          <Header
            headerTitle="New List"
            rightAction={{ icon: "check", onPress: handleAddList }}
          />
          <View style={styles.inputArea}>
            <TextInput
              autoFocus
              value={newListTitle}
              onChangeText={setNewListTitle}
              placeholder="List name"
              placeholderTextColor={dividerColor}
              onSubmitEditing={handleAddList}
              style={[styles.modalInput, { color: textColor, borderBottomColor: textColor }]}
              allowFontScaling={false}
            />
          </View>
        </View>
      </Modal>

      {/* Rename modal */}
      <Modal visible={showRenameModal} animationType="none" transparent statusBarTranslucent>
        <View style={[styles.inputBackdrop, { backgroundColor: bg }]}>
          <Header
            headerTitle="Rename"
            rightAction={{ icon: "check", onPress: handleRename }}
          />
          <View style={styles.inputArea}>
            <TextInput
              autoFocus
              value={renameTitle}
              onChangeText={setRenameTitle}
              onSubmitEditing={handleRename}
              placeholder="List name"
              placeholderTextColor={dividerColor}
              style={[styles.modalInput, { color: textColor, borderBottomColor: textColor }]}
              allowFontScaling={false}
            />
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scroll: {
    flex: 1,
  },
  listItem: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: n(22),
    paddingVertical: n(16),
    borderBottomWidth: n(1),
  },
  listItemLeft: {
    flex: 1,
  },
  listTitle: {
    fontSize: n(30),
  },
  listItemRight: {
    flexDirection: "row",
    alignItems: "center",
    gap: n(8),
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
  // Action menu
  menuBackdrop: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.5)",
    justifyContent: "center",
    alignItems: "center",
    paddingHorizontal: n(40),
  },
  menuCard: {
    width: "100%",
    borderWidth: n(1),
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
    borderTopWidth: n(1),
  },
  menuItemText: {
    fontSize: n(24),
  },
  menuItemDelete: {
    opacity: 0.4,
  },
  // Input modals
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
    borderBottomWidth: n(1),
    paddingBottom: n(8),
  },
});
