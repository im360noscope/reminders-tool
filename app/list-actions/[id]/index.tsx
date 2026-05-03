import { router, useLocalSearchParams } from "expo-router";
import { StyleSheet } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Header } from "@/components/Header";
import { HapticPressable } from "@/components/HapticPressable";
import { StyledText } from "@/components/StyledText";
import { SwipeBackContainer } from "@/components/SwipeBackContainer";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { useReminders } from "@/contexts/RemindersContext";
import { n } from "@/utils/scaling";

export default function ListActionsScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { invertColors } = useInvertColors();
  const { lists } = useReminders();
  const bg = invertColors ? "white" : "black";

  const list = lists.find(l => l.id === id);
  const listTitle = list?.title ?? "List";

  const handleReorder = () => {
    // Go back to lists with reorder mode — pass param
    router.navigate({ pathname: "/(tabs)/", params: { startReorder: "true" } });
  };

  return (
    <SwipeBackContainer onSwipeBack={() => router.back()}>
      <SafeAreaView style={[styles.container, { backgroundColor: bg }]} edges={["top"]}>
        <Header headerTitle={listTitle} />

        <HapticPressable
          onPress={() => router.push({ pathname: "/list-actions/[id]/rename", params: { id } })}
          style={styles.option}
        >
          <StyledText style={styles.optionText}>Rename</StyledText>
        </HapticPressable>

        <HapticPressable onPress={handleReorder} style={styles.option}>
          <StyledText style={styles.optionText}>Reorder</StyledText>
        </HapticPressable>

        <HapticPressable
          onPress={() => router.push({
            pathname: "/confirm",
            params: {
              title: listTitle,
              message: "Tasks will be moved to your default list.",
              confirmText: "Delete",
              action: `delete-list:${id}`,
              returnPath: "/(tabs)/",
            },
          })}
          style={styles.option}
        >
          <StyledText style={[styles.optionText, styles.destructive]}>Delete</StyledText>
        </HapticPressable>

      </SafeAreaView>
    </SwipeBackContainer>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  option: {
    paddingHorizontal: n(22),
    paddingVertical: n(14),
  },
  optionText: {
    fontSize: n(30),
  },
  destructive: {
    opacity: 0.4,
  },
});
