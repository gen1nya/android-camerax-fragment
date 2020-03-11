
## cameraX beta01 demo app and simple library

### Key features:
- taking photos with fixed resolution (1280 x 720)
- linear zoom (slider/seekbar)
- tap-to-focus (focus + exposition + WB)
- flashlight

### TODO:
- lens changing (face/back/telephoto/ultrawide)
- resolution selector and aspect ratio selector
- pich-to-zoom

### library integration

1. Add it in your root build.gradle at the end of repositories:

```
allprojects {
  repositories {
    maven { url 'https://jitpack.io' }
  }
}
```

2. Add the dependency
```
dependencies {
  implementation 'com.github.gen1nya:android-camerax-fragment:-SNAPSHOT'
}
  ```

3. Add fragment:

```
val fragment = CameraFragment()
supportFragmentManager.beginTransaction()
  .add(R.id.flFragmentContainer, fragment, "CameraFragment")
  .commitAllowingStateLoss()
```

4. Add listener 

`fragment.setCameraListener(object : CameraFragment.CameraListener {... `

5. Take photo:

`fragment.takePicture()`
