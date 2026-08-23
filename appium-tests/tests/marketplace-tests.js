describe('Marketplace Module', () => {
    it('APP-101: User can view skill details', async () => {
        const marketplaceTab = await $('~Marketplace Tab');
        await marketplaceTab.waitForDisplayed();
        await marketplaceTab.click();
        
        const firstSkillCard = await $('~Skill Card 0'); 
        await firstSkillCard.waitForDisplayed();
        await firstSkillCard.click();
        
        const skillDetailsTitle = await $('~Skill Details Title');
        await expect(skillDetailsTitle).toBeDisplayed();
    });

    it('APP-102: Insufficient credits warning', async () => {
        // Navigate to a premium skill and try to enroll
        const enrollButton = await $('~Enroll Button');
        if(await enrollButton.isDisplayed()) {
             await enrollButton.click();
             const insufficientCreditsMsg = await $('~Insufficient Credits Message');
             await expect(insufficientCreditsMsg).toBeDisplayed();
        }
    });
});
