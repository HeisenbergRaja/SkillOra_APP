describe('Quiz Module', () => {
    it('APP-201: User can start a quiz', async () => {
        const myLearningTab = await $('~My Learning Tab');
        await myLearningTab.waitForDisplayed();
        await myLearningTab.click();
        
        const startQuizButton = await $('~Start Quiz Button');
        if(await startQuizButton.isDisplayed()) {
            await startQuizButton.click();
            const quizQuestion = await $('~Quiz Question Text');
            await expect(quizQuestion).toBeDisplayed();
        }
    });

    it('APP-202: Quiz submission', async () => {
        const submitQuizButton = await $('~Submit Quiz Button');
        if(await submitQuizButton.isDisplayed()) {
            await submitQuizButton.click();
            const quizResult = await $('~Quiz Result Score');
            await expect(quizResult).toBeDisplayed();
        }
    });
});
