import { router } from "expo-router";
import { useCallback, useState } from "react";
import { KeyboardAvoidingView, Platform, StyleSheet, TextInput, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Header } from "@/components/Header";
import { SwipeBackContainer } from "@/components/SwipeBackContainer";
import { useInvertColors } from "@/contexts/InvertColorsContext";
import { useReminders } from "@/contexts/RemindersContext";
import { n } from "@/utils/scaling";

export default function NewListScreen() {
  const { invertColors } = useInvertColors();
  const { addList } = useReminders();
  const bg = invertColors ? "white" : "black";
  const textColor = invertColors ? "black" : "white";
  const dimColor = invertColors ? "#AAAAAA" : "#555555";

  const [title, setTitle] = useState("");
  const canSave = title.trim().length > 0;

  const handleSave = useCallback(() => {
    if (!canSave) return;
    addList(title.trim());
    router.back();
  }, [canSave, title, addList]);

  return (
    <SwipeBackContainer onSwipeBack={() => router.back()}>
      <View style={[styles.fill, { backgroundColor: bg }]}>
        <KeyboardAvoidingView style={styles.fill} behavior="padding">
          <SafeAreaView style={styles.fill} edges={["top"]}>
            <Header
              headerTitle="New List"
              rightAction={{ icon: "check", onPress: handleSave, show: canSave }}
            />
            <View style={styles.inputArea}>
              <TextInput
                autoFocus
                value={title}
                onChangeText={setTitle}
                placeholder="List name"
                placeholderTextColor={dimColor}
                onSubmitEditing={handleSave}
                style={[styles.input, { color: textColor }]}
                allowFontScaling={false}
                returnKeyType="done"
              />
            </View>
          </SafeAreaView>
        </KeyboardAvoidingView>
      </View>
    </SwipeBackContainer>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  inputArea: { paddingHorizontal: n(22), paddingTop: n(24) },
  input: { fontSize: n(30), fontFamily: "PublicSans-Regular", paddingBottom: n(8) },
});
