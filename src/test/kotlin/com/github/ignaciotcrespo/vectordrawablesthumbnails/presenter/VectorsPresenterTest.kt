package com.github.ignaciotcrespo.vectordrawablesthumbnails.presenter

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.github.ignaciotcrespo.vectordrawablesthumbnails.parser.IVectorDrawableParser
import com.github.ignaciotcrespo.vectordrawablesthumbnails.scanners.IProjectFileScanner
import com.github.ignaciotcrespo.vectordrawablesthumbnails.utils.Utils
import com.intellij.openapi.project.Project
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.awt.image.BufferedImage
import java.io.File

@ExtendWith(MockitoExtension::class)
class VectorsPresenterTest {

    @Mock
    lateinit var projectFileScanner: IProjectFileScanner
    @Mock
    lateinit var vectorDrawableParser: IVectorDrawableParser
    @Mock
    lateinit var project: Project

    lateinit var presenter: VectorsPresenter
    private lateinit var testSchedulerEvents: TestObserver<PresenterEvent>

    private lateinit var mockStaticUtils: MockedStatic<Utils>

    // Mock items for testing
    private val mockImage1: BufferedImage = mock()
    private val mockImage2: BufferedImage = mock()
    private val mockImage3: BufferedImage = mock()

    private val mockFile1: File = mock()
    private val mockFile2: File = mock()
    private val mockFile3: File = mock()

    private val mockValidFile1: ValidFile = mock()
    private val mockValidFile2: ValidFile = mock()
    private val mockValidFile3: ValidFile = mock()

    private val item1 = VectorItem("apple.xml", mockImage1, mockValidFile1, 10, 20, 100L)  // name, width, height, size
    private val item2 = VectorItem("banana.xml", mockImage2, mockValidFile2, 20, 10, 50L)
    private val item3 = VectorItem("cherry.xml", mockImage3, mockValidFile3, 10, 10, 150L)


    @BeforeEach
    fun setUp() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setNewThreadSchedulerHandler { Schedulers.trampoline() }
        // If other schedulers are used by the presenter, override them too. (Not aware of others for now)

        mockStaticUtils = Mockito.mockStatic(Utils::class.java)

        presenter = VectorsPresenter(projectFileScanner, vectorDrawableParser)
        testSchedulerEvents = presenter.getPresenterEvents().test()

        // Setup mock files for items
        whenever(mockValidFile1.file).thenReturn(mockFile1)
        whenever(mockValidFile2.file).thenReturn(mockFile2)
        whenever(mockValidFile3.file).thenReturn(mockFile3)
        // If file names are used by items (they are, via item.name), mock them too
        whenever(mockFile1.name).thenReturn("apple.xml")
        whenever(mockFile2.name).thenReturn("banana.xml")
        whenever(mockFile3.name).thenReturn("cherry.xml")
    }

    @AfterEach
    fun tearDown() {
        RxJavaPlugins.reset()
        mockStaticUtils.close()
    }

    // --- refreshPropertiesData Tests ---

    @Test
    fun `refreshPropertiesData successful refresh`() {
        val mockValidFile = mock<ValidFile>()
        val mockVectorItem = mock<VectorItem>()

        whenever(projectFileScanner.findXmlFiles(project)).thenReturn(Observable.just(mockValidFile))
        whenever(vectorDrawableParser.parseVector(any(ValidFile::class.java))).thenReturn(Observable.just(mockVectorItem))

        presenter.refreshPropertiesData(project, delay = false)

        testSchedulerEvents.assertValueAt(0) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.SEARCHING }
        testSchedulerEvents.assertValueAt(1) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.IDLE }
        assert(presenter.itemsFiltered().contains(mockVectorItem))
        testSchedulerEvents.assertNoErrors() // No RxJava errors on the presenterEvents stream
    }

    @Test
    fun `refreshPropertiesData projectFileScanner emits error`() {
        whenever(projectFileScanner.findXmlFiles(project)).thenReturn(Observable.error(RuntimeException("Scanner error")))

        presenter.refreshPropertiesData(project, delay = false)

        testSchedulerEvents.assertValueAt(0) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.SEARCHING }
        testSchedulerEvents.assertValueAt(1) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.IDLE }
        assert(presenter.itemsFiltered().isEmpty())
        // The internal error is caught and state is set to IDLE. No error should propagate to presenterEvents observer.
        testSchedulerEvents.assertNoErrors()
    }

    @Test
    fun `refreshPropertiesData vectorDrawableParser emits error`() {
        val mockValidFile = mock<ValidFile>()
        whenever(projectFileScanner.findXmlFiles(project)).thenReturn(Observable.just(mockValidFile))
        whenever(vectorDrawableParser.parseVector(any(ValidFile::class.java))).thenReturn(Observable.error(RuntimeException("Parser error")))

        presenter.refreshPropertiesData(project, delay = false)

        testSchedulerEvents.assertValueAt(0) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.SEARCHING }
        testSchedulerEvents.assertValueAt(1) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.IDLE }
        assert(presenter.itemsFiltered().isEmpty())
        testSchedulerEvents.assertNoErrors()
    }
    
    @Test
    fun `refreshPropertiesData vectorDrawableParser returns empty for an item`() {
        val mockValidFile = mock<ValidFile>()
        whenever(projectFileScanner.findXmlFiles(project)).thenReturn(Observable.just(mockValidFile))
        whenever(vectorDrawableParser.parseVector(any(ValidFile::class.java))).thenReturn(Observable.empty()) // or Observable.just(null)

        presenter.refreshPropertiesData(project, delay = false)
        
        testSchedulerEvents.assertValueAt(0) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.SEARCHING }
        testSchedulerEvents.assertValueAt(1) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.IDLE }
        assert(presenter.itemsFiltered().isEmpty())
        testSchedulerEvents.assertNoErrors()
    }

    // --- filter and itemsFiltered Tests ---
    private fun populatePresenterWithTestData() {
        whenever(projectFileScanner.findXmlFiles(project)).thenReturn(Observable.just(mockValidFile1, mockValidFile2, mockValidFile3))
        whenever(vectorDrawableParser.parseVector(mockValidFile1)).thenReturn(Observable.just(item1))
        whenever(vectorDrawableParser.parseVector(mockValidFile2)).thenReturn(Observable.just(item2))
        whenever(vectorDrawableParser.parseVector(mockValidFile3)).thenReturn(Observable.just(item3))
        presenter.refreshPropertiesData(project, delay = false)
        // Clear events from refresh before filter/sort tests
        testSchedulerEvents = presenter.getPresenterEvents().test() 
    }

    @Test
    fun `filter items by name`() {
        populatePresenterWithTestData()
        presenter.filter("apple")
        var filtered = presenter.itemsFiltered()
        assert(filtered.size == 1 && filtered.contains(item1))

        presenter.filter("BANANA") // Test case insensitivity
        filtered = presenter.itemsFiltered()
        assert(filtered.size == 1 && filtered.contains(item2))

        presenter.filter(null)
        filtered = presenter.itemsFiltered()
        assert(filtered.size == 3)

        presenter.filter("")
        filtered = presenter.itemsFiltered()
        assert(filtered.size == 3)

        presenter.filter("nonexistent")
        filtered = presenter.itemsFiltered()
        assert(filtered.isEmpty())
    }

    // --- sortBy2, sortByDirection, and itemsFiltered Tests ---
    @Test
    fun `sort items by Name`() {
        populatePresenterWithTestData()
        presenter.sortBy2("By Name")

        presenter.sortByDirection("Asc")
        var sorted = presenter.itemsFiltered()
        assert(sorted.map { it.name } == listOf("apple.xml", "banana.xml", "cherry.xml"))

        presenter.sortByDirection("Desc")
        sorted = presenter.itemsFiltered()
        assert(sorted.map { it.name } == listOf("cherry.xml", "banana.xml", "apple.xml"))
    }

    @Test
    fun `sort items by Width`() {
        populatePresenterWithTestData()
        presenter.sortBy2("By Width")

        presenter.sortByDirection("Asc")
        var sorted = presenter.itemsFiltered() // item1 (10), item3 (10), item2 (20)
        assert(sorted.map { it.name } == listOf("apple.xml", "cherry.xml", "banana.xml") || sorted.map { it.name } == listOf("cherry.xml", "apple.xml", "banana.xml"))


        presenter.sortByDirection("Desc")
        sorted = presenter.itemsFiltered() // item2 (20), item1 (10), item3 (10)
        assert(sorted.map { it.name } == listOf("banana.xml", "apple.xml", "cherry.xml") || sorted.map { it.name } == listOf("banana.xml", "cherry.xml", "apple.xml"))
    }
    
    @Test
    fun `sort items by Height`() {
        populatePresenterWithTestData()
        presenter.sortBy2("By Height")

        presenter.sortByDirection("Asc") // item2 (10), item3 (10), item1 (20)
        var sorted = presenter.itemsFiltered()
        assert(sorted.map { it.name } == listOf("banana.xml", "cherry.xml", "apple.xml") || sorted.map { it.name } == listOf("cherry.xml", "banana.xml", "apple.xml"))

        presenter.sortByDirection("Desc") // item1 (20), item2 (10), item3 (10)
        sorted = presenter.itemsFiltered()
        assert(sorted.map { it.name } == listOf("apple.xml", "banana.xml", "cherry.xml") || sorted.map { it.name } == listOf("apple.xml", "cherry.xml", "banana.xml"))
    }

    @Test
    fun `sort items by Width x Height`() {
        populatePresenterWithTestData() // item1 (200), item2 (200), item3 (100)
        presenter.sortBy2("By Width x Height")

        presenter.sortByDirection("Asc") // item3 (100), item1 (200), item2 (200)
        var sorted = presenter.itemsFiltered()
        assert(sorted[0].name == "cherry.xml")
        assert( (sorted[1].name == "apple.xml" && sorted[2].name == "banana.xml") || (sorted[1].name == "banana.xml" && sorted[2].name == "apple.xml") )


        presenter.sortByDirection("Desc") // item1 (200), item2 (200), item3 (100)
        sorted = presenter.itemsFiltered()
        assert( (sorted[0].name == "apple.xml" && sorted[1].name == "banana.xml") || (sorted[0].name == "banana.xml" && sorted[1].name == "apple.xml") )
        assert(sorted[2].name == "cherry.xml")
    }

    @Test
    fun `sort items by File Size`() {
        populatePresenterWithTestData() // item1 (100L), item2 (50L), item3 (150L)
        presenter.sortBy2("By File Size")

        presenter.sortByDirection("Asc") // item2 (50L), item1 (100L), item3 (150L)
        var sorted = presenter.itemsFiltered()
        assert(sorted.map { it.name } == listOf("banana.xml", "apple.xml", "cherry.xml"))

        presenter.sortByDirection("Desc") // item3 (150L), item1 (100L), item2 (50L)
        sorted = presenter.itemsFiltered()
        assert(sorted.map { it.name } == listOf("cherry.xml", "apple.xml", "banana.xml"))
    }
    
    @Test
    fun `sort with no sort criteria or invalid sort criteria`() {
        populatePresenterWithTestData()
        val initialOrder = presenter.itemsFiltered().toList() // Capture initial order (which might be somewhat arbitrary from mocks)

        presenter.sortBy2("Invalid Sort") // Invalid criteria
        presenter.sortByDirection("Asc")
        var sorted = presenter.itemsFiltered()
        assert(sorted == initialOrder) // Order should not change

        presenter.sortBy2(null.toString()) // Null criteria (converted to string "null")
        presenter.sortByDirection("Asc")
        sorted = presenter.itemsFiltered()
        assert(sorted == initialOrder) // Order should not change
    }


    // --- onVectorClicked Test ---
    @Test
    fun `onVectorClicked calls Utils_openValidFile`() {
        val mockItem = mock<VectorItem>()
        val mockValidFileClicked = mock<ValidFile>()
        whenever(mockItem.validFile).thenReturn(mockValidFileClicked)

        presenter.onVectorClicked(project, mockItem)

        mockStaticUtils.verify { Utils.openValidFile(project, mockValidFileClicked) }
    }
}
