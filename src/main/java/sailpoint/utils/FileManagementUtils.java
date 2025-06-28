package sailpoint.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import sailpoint.exception.FileReadException;

public class FileManagementUtils {
	private static Logger logger = Logger.getInstance();
	public static List<File> loadFiles(String[] files) throws FileReadException {
		logger.debug(String.format("loadFiles: Loading %s files...", files.length));
//		try {
			ArrayList<File> fileList = new ArrayList<File>();
			
			for (int index = 0; index < files.length; index++) {
				File file = new File(files[index]);
				
				logger.debug(String.format("loadFiles: Loading %s...", file.getName()));
				
				if (!file.exists()) {
					throw new FileReadException(String.format("File '%s' not found.", file));
				}
				
				fileList.add(file);
			}
			return fileList;
//		}
	}
	
	
}
