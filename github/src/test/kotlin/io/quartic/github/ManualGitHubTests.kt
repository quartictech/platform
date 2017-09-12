package io.quartic.github

import io.quartic.common.application.DEV_MASTER_KEY_BASE64
import io.quartic.common.client.ClientBuilder
import io.quartic.common.secrets.EncryptedSecret
import io.quartic.common.secrets.SecretsCodec
import org.junit.Ignore
import org.junit.Test
import java.net.URI

@Ignore
class ManualGitHubTests {
    private val client = GitHubInstallationClient(
        appId = "4352",
        githubApiRoot = URI("https://api.github.com"),
        key = SecretsCodec(DEV_MASTER_KEY_BASE64).decrypt(EncryptedSecret("1\$fa62753b669b42c98c47f9bd\$1e2fe860bf8a8c2aef5ce5d65d869df93d5c0802155fd0bf21fb9e59b3cec714665c5781b3328327aa0b727bb8c0735502e7fa6955cb2ef0b4cdca42547207a159f97da3d6d0c41f1c6fa608e6a139acc1bf2c9997022f69145c5034c436731dfe4f32e5678c1359187aeaf831c8a3565be5047cb095a105eb242b1f26f7e49ef85ca44bd007d8f5c8ceed4e1f5b13f94f7f4993a00fefd4e0328dd1a9aeb65c7f40121eddd0383011c9314d196839414c4eb19243d5e34c487a2ec85ebdcd35b59913d884559ab73455ec482e2e7ab3e2a49619d99ab2b47ff7a911918c4da06269547b3ac7914627027b851055d3f59c8f06f877a1840a5175db08af51dff314de41aad3d67dcda351bc5172813e9cabbc67ab3b029c34457bf66d3d13ef6c4c76cac7123bc12dfcc276eddde9e1bb6191fc009bca19eb51c1a344fb5d7d643afcc2d6073ce210c0382de89892142e94f1f2503d113d1e2a1b5672938e26c4a5ac84c2d36bb6e0721805eda8924d2eebf8e48560feefe1addd360c4b2746fe2a2ba1a1ce1caaa6d15253159e7745d6e290020e047f83889b66809477b12400cdc70778958ec6c8a1f167cf22c8daee3735ea8168ef03e5c35729407681299bbb24ceed277c84f46e293d8c05633abb034033c3b2a158f91782620abef7e96d106cde2710641ba6111be8e60ccc301439a5c682143a6993544873fa9db187270cbad8c882b8cb73bd71b2d9823bf4a4248d15a0768c58c343a6c5552a8ef62596b3d3997d210e8154bd06ccd12ee40b9c2aabd2bdbbdebf7c4871160282c0633a930c1aaf1031ae1981589fa2dc1677238f462b86198ba2fa7b427f14f16f1da2c17e3fdb7c827a3709b0327bf206936c9eaa993d863c40e7a3c3d1ae22c37c3e43ea96f46e7b016617a02e45a1294e941b4bf29557cfcc8128348b3f5ef7c89277d5b9510b7dd93bf3a38e846155fd8281a04afbdcebabb9bc43a55bd80665a406413247989ae57413621746071809250b1651538aeedc7861a0cda1f570637ff0ccd8ce5e5f2dfbba32b467aabf9c9c827cc41477894bb32fb0b05954b39663cef0240b07beb123183db53446eefc3bd8c32a54b757e43184883ceb8ac5e5752ef475fce118da536a4a51854378edaedf6c533f4c2154e1180543d5013eea0d4fdb35401f3fbac5f78615cfdba0a71b34818b0bbd4ebdd77d1f09ab17cc4868771b6bc49c4ee817f2cf397524e41181620a62cec7e80d28ce37077cbd6e63daacfa980ad2fd11eeeb889330ed5f6b0b216af4d3f1f80ddd66350c1043ac1d4504c5aecb724169ead122e63d845e32d8dfea34830eb900b7c0bf42a8e83ea23e052feff48544510ebb713cf2c262cb2d884b4d5bf5282ad4b08e24e5ce4f102fe60bd77feb914259cecb0d653ca82f824db60a566e594493f93c419dc611764ea839be8fd01c88ab18c1e62f527d4db138f4bb5d55a4f9a4f810f562a3a99ed80ae2a535a90035a8c01f8cd8ef68f3352c8938c40c4754a02236e4b5dee4c43f8d176bff781fccafd85774428116132ab4d3a53e872dbb7a66fc7a55d55d7e2d9248717fb6cb1378ea6d496495b6f714fab34aa1cb1280eac8658440ce06a5d84d8c460bf48a840bd223d39b96ecce50e6844a1e35a19534eafe6582d00c8de93f1e559839dbeb62b34622abeba34648751aeebf6aba9fd2e0e300f58cccfaf71bde4757e73e65b00ab21cdfe413050e823012b762addcd67553d0bb2262574991afda7c347521d21a7637cc9cdadd7608438f12498ef7e65f13ca4d277b70acfd5c6bc1885f3fc3882948eef46e7d466e5d90923ba7d95e214cc8e3a76e25057f344718cf37df132981fdc1a58f033222045eaed347a8092a04b06974558f01ab30fb6c82f38a21d88fd2eda9b8889b497e7317b78f34f78d36df83bbb97f4287882654c724b3859880d7d2d78b40c78eaebd51d1e405e58092229e2593913a605e6788071addaa9b10d5ba7227adee3f8b06c27447e8eb8096d2d750f4a4dba4387f31dc52afce1958b745afcb79bf8d35bc54a39cd93ee605bd6d29055b50cc2fd4a7781934fe0ae208854714b90df7758c9a95bfceee8669cc899fd4aca4c5b4ef01846ab191f9fad4bfe177a861e538207287ac77467db85d9e3fc12b6d1f37dbeb86550e1c4a5f7d0a76238461b8d522f71888421bfb44e8826523a147c327f9132e754fa4745e4d5be7cb39008909010ffb604fb6de6416a1cbc8b19c406a5fab1048356a4e76269496dee56ebbbc8064d0584d5f3c45b8bcca688d00b0bfde4094a986e503acdd2ec60cc1d3d5\$5cee14815e372623f1d041fdb0beffae")),
        clientBuilder = ClientBuilder(javaClass)
    )

    private val installationId: Long = 45267

    @Test
    fun with_retrofit() {
        println(client.accessTokenAsync(installationId).get())
    }

    @Test
    fun send_status_updates() {
         val accessToken = client.accessTokenAsync(45267).get()
         client.sendStatusAsync(
             "quartictech",
             "playground",
             "c98a7f98f6f05dd6ed5cf66aa9f07e10a78ba2c6",
             StatusCreate(
                 "success",
                 URI.create("https://playground.quartic.io"),
                 "success",
                 "quartic"
             ),
             accessToken
         ).join()
     }

    @Test
    fun fetch_repository() {
        val accessToken = client.accessTokenAsync(45267).get()
        val repo = client.getRepositoryAsync(78424428, accessToken).get()
        println(repo)
     }
}
