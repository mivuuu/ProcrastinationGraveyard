from pathlib import Path
import xml.etree.ElementTree as ET
import os
root=Path(os.environ.get('GRADLE_USER_HOME',str(Path.home()/'.gradle')))/'caches/modules-2/files-2.1'
records=[]
for group in ['androidx.activity','androidx.compose.animation','androidx.compose.foundation','androidx.compose.material','androidx.compose.material3','androidx.compose.runtime','androidx.compose.ui','androidx.core','androidx.navigation','androidx.lifecycle','androidx.hilt','androidx.room','androidx.datastore','androidx.work','com.google.dagger','io.github.sceneview','com.google.android.filament','dev.romainguy','org.jetbrains.kotlin','org.jetbrains.kotlinx','com.google.guava','com.google.code.findbugs','com.google.errorprone','org.checkerframework','org.jetbrains','javax.inject','com.squareup','com.squareup.okio','com.google.android','androidx.annotation','androidx.arch.core','androidx.collection','androidx.concurrent','androidx.graphics','androidx.savedstate','androidx.startup','androidx.tracing','androidx.profileinstaller','androidx.interpolator','androidx.sqlite','androidx.transition','androidx.vectordrawable','androidx.versionedparcelable','androidx.viewpager','androidx.loader','androidx.customview','androidx.cursoradapter','androidx.fragment','androidx.emoji2','androidx.appcompat','androidx.asynclayoutinflater','androidx.constraintlayout','androidx.lifecycle','androidx.resourceinspection','com.google.j2objc','com.google.jimfs']:
 for pom in (root/group).glob('*/*/*/*.pom'):
  try:
   doc=ET.parse(pom).getroot();ns={'m':'http://maven.apache.org/POM/4.0.0'}
   licenses=[(e.findtext('m:name',namespaces=ns),e.findtext('m:url',namespaces=ns)) for e in doc.findall('m:licenses/m:license',ns)]
   if licenses:records.append((group,pom.parents[2].name,pom.parents[1].name,licenses))
  except ET.ParseError:pass
out=['# Third-party license audit\n','License declarations read from the resolved Maven POM files. Build and test artifacts may also appear. Application art and geometry are original; no external models, images, or fonts are bundled. Android platform fonts are used.\n','| Group:artifact | Version | Declared license |','|---|---|---|']
for g,a,v,ls in sorted(set((g,a,v,tuple(ls)) for g,a,v,ls in records)):
 out.append(f'| {g}:{a} | {v} | '+', '.join(f'[{name}]({url})' for name,url in ls)+' |')
Path('THIRD_PARTY_LICENSES.md').write_text('\n'.join(out)+'\n')
